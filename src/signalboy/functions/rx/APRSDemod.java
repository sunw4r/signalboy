package signalboy.functions.rx;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import signalboy.audio.CaptureAudioDevice;
import signalboy.audio.Goertzel;
import signalboy.audio.WindowFunction;

public class APRSDemod extends RX {

    @FXML TextArea textAreaDecode;

    public enum State {
        WAITING_FOR_PACKET,
        DECODING_ADDRESS
    }

    private int FREQ_MARK = 1200;
    private int FREQ_SPACE = 2200;
    private int BAUD = 1200;

    private double THRESHOLD_MARK = 16; //Get this number with tests, don't know if is correct
    private double THRESHOLD_SPACE = 16;

    private boolean NRZI_HIGH = false;
    private int bitStuffCounter = 0;
    private int bitSequenceIndex = 0;
    private boolean[] bitSequence = new boolean[8];

    private State currentState;
    private Goertzel goertzelMark;
    private Goertzel goertzelSpace;
    private int N;
    private int Nborder;

    private short[] partsBuffer;
    private int totalParts = 4;
    private boolean checkSample = false;

    //FXML call
    public void initialize() {

        textAreaDecode.setText("Initializing...");
        currentState = State.WAITING_FOR_PACKET;

        N = CaptureAudioDevice.SAMPLE_RATE/BAUD;
        Nborder = N / 2;
        partsBuffer = new short[Nborder*4];

        goertzelMark = new Goertzel(FREQ_MARK, CaptureAudioDevice.SAMPLE_RATE);
        goertzelMark.init();

        goertzelSpace = new Goertzel(FREQ_SPACE, CaptureAudioDevice.SAMPLE_RATE);
        goertzelSpace.init();

    }

    @Override
    public void onStop() {

    }

    @Override
    public void processBuffer(short[] buffer) {

        String secondString = "";

        for (int i = 0; i <= buffer.length-(Nborder); i+=Nborder) {

            // Dividing the buffer into 4 parts of N/2, to process the goertzel with N/2 window of each side.
            // Window Left N/2 parts ---- Target Samples N parts ----- Window Right N/2 parts
            // This is probably insanely slow, but i'm using a PC and learning, so i think its acceptable :-P
            for (int ix = totalParts-1; ix >= 0; ix--) {
                for (int iy = 0; iy < Nborder; iy++) {
                    int targetIndex = (i - (((totalParts-1)-ix)*Nborder))+iy;
                    if (targetIndex >= 0) {
                        partsBuffer[ix*Nborder+iy] = buffer[targetIndex];
                    }
                    //System.out.println(targetIndex+" : "+ix*Nborder+iy);
                }
            }

            checkSample = !checkSample;

            if (checkSample) {

                //System.out.println(i);

                boolean signalLost = false;

                int result = 0;
                if (NRZI_HIGH) {
                    result = 1;
                }

                //Update Bit for next iteration
                short[] demodBuffer = new short[partsBuffer.length];
                for (int ii = 0; ii < partsBuffer.length; ii++) {
                    demodBuffer[ii] = (short) ((double) partsBuffer[ii] * WindowFunction.hamming(ii, partsBuffer.length));
                }

                int nrziCodedBit = demodulate(demodBuffer, 0, demodBuffer.length);

                if (nrziCodedBit == 1) {
                    NRZI_HIGH = !NRZI_HIGH;
                } else if (nrziCodedBit == -1) {
                    //Lost Signal
                    signalLost = true;
                    NRZI_HIGH = false;
                }

                //decode
                if (nrziCodedBit == 1 && !signalLost) {
                    secondString += "1";
                } else if (nrziCodedBit == 0 && !signalLost) {
                    secondString += "0";
                } else if (signalLost) {
                    secondString += "-";
                }

                if (!signalLost) {

                    boolean binary = false;

                    if (result > 0) {

                        binary = true;

                    }

                    if (currentState == State.WAITING_FOR_PACKET) {

                        //No need for bitstuffing check
                        searchForPacket(binary);

                    } else {

                        //Check for stuffing

                        if (bitStuffCounter == 5 && !binary) {
                            bitStuffCounter = 0;
                            return;
                        } else if (binary) {
                            bitStuffCounter++;
                        }

                        if (currentState == State.DECODING_ADDRESS) {

                            decodeAdress(binary);

                        }

                    }

                } else {
                    if (currentState != State.WAITING_FOR_PACKET) {
                        changeState(State.WAITING_FOR_PACKET);
                    }
                }
            }
        }

        if (secondString.length() > 0) {
            System.out.println(secondString);
        }

    }

    private void changeState(State state) {

        bitSequenceIndex = 0;
        bitSequence = new boolean[8];
        currentState = state;

    }

    private void decodeAdress(boolean binary) {

        bitSequence[bitSequenceIndex] = binary;
        bitSequenceIndex++;

        if (bitSequenceIndex == bitSequence.length) {
            bitSequenceIndex = 0;

            int constrByte = 0;
            int l = bitSequence.length;

            for (int i = 0; i < l; ++i) {
                constrByte = (constrByte << 1) + (bitSequence[i] ? 1 : 0);
            }

            //System.out.println(Character.toString((char) constrByte));
        }

    }

    private void searchForPacket(boolean binary) {

        bitSequence[bitSequenceIndex] = binary;

        /*for (int i = 0; i < bitSequence.length; i++) {

            boolean b = bitSequence[i];
            if (b) {
                flagByte = flagByte << 1;
                flagByte = flagByte + 0x01;
            } else {
                flagByte = flagByte << 1;
            }

        }*/

        int flagByte = 0;
        int l = bitSequence.length;

        for (int i = 0; i < l; ++i) {
            flagByte = (flagByte << 1) + (bitSequence[i] ? 1 : 0);
        }

        bitSequenceIndex++;
        if (bitSequenceIndex == bitSequence.length) {
            bitSequenceIndex = 0;
        }

        //System.out.println(String.format("0x%08X", flagByte));
        if (flagByte == 0x7E) {
           // System.out.println(Arrays.toString(bitSequence));
            //System.out.println("Packet Found!");
            changeState(State.DECODING_ADDRESS);
        }

    }

    private int demodulate (short[] data, int idx, int binSize) {

        // Detecting the power of the signal,
        // to compare with the goertzelOld magnitude and get a relative value
        double sum_sqr = 0;

        for (int ii = 0; ii < binSize; ii++) {

           // System.out.println(idx+ii);
            goertzelMark.processSample((float)data[idx+ii]);
            goertzelSpace.processSample((float)data[idx+ii]);
            sum_sqr += (double)data[idx+ii] * (double)data[idx+ii];

        }

        double power = sum_sqr / (double)binSize;
        double rms = (double) Math.sqrt(power);

        double magnitudeMark = Math.sqrt(goertzelMark.getMagnitude());
        double magnitudeSpace = Math.sqrt(goertzelSpace.getMagnitude());

        double markXrms = (magnitudeMark/rms);
        double spaceXrms = (magnitudeSpace/rms);

        goertzelMark.reset();
        goertzelSpace.reset();

        //System.out.printf("mark:%f\n",(markXrms));
        //System.out.printf("space:%f\n",(spaceXrms));
        //System.out.println("-------------------------");

        if (markXrms >= THRESHOLD_MARK) {
            return 1;
        }

        if (spaceXrms >= THRESHOLD_SPACE) {
            return 0;
        }
        return -1;

    }

}
