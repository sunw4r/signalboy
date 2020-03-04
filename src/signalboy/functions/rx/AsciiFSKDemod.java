package signalboy.functions.rx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import signalboy.audio.*;
import signalboy.custom.ResizableCanvas;

import java.io.Console;
import java.util.Arrays;

public class AsciiFSKDemod extends RX {

    static int STATE_PACKET_SEARCH = 0;
    static int STATE_PACKET_HEADER = 1;
    static int STATE_PACKET_MSGSIZE = 2;
    static int STATE_PACKET_MSGDECODING = 3;

    @FXML ResizableCanvas canvasSignal;
    @FXML ResizableCanvas canvasMark;
    @FXML ResizableCanvas canvasSpace;

    @FXML TextField textFieldMark;
    @FXML TextField textFieldSpace;
    @FXML TextField textFieldBaud;
    @FXML TextArea textAreaMessage;
    @FXML Label labelResumeFreq;
    @FXML Button buttonUpdate;

    private String binaryHeader = "";
    private String binaryMsgSize = "";
    private short msgSize = 0;
    private String binaryMsg = "";

    int currentState = STATE_PACKET_SEARCH;
    int markHz = 1200;
    int spaceHz = 2200;
    int baud = 1200;
    double symbolSize = (double)CaptureAudioDevice.SAMPLE_RATE/(double)baud;

    Goertzel goertzelMark;
    Goertzel goertzelSpace;
    int N;

    ConvolutionFreq convMark;
    ConvolutionFreq convSpace;
    ConvolutionPattern convPreamble;

    private AudioDisplay mSignalDisplay;
    private AudioDisplay mMarkDisplay;
    private AudioDisplay mSpaceDisplay;

    private final String preamble = "01010101";
    private final String headerChar = ":";

    @Override
    public void initialize() {

        mSignalDisplay = new AudioDisplay(canvasSignal);
        mMarkDisplay = new AudioDisplay(canvasMark);
        mMarkDisplay.setLineColor(Color.RED);
        mSpaceDisplay = new AudioDisplay(canvasSpace);
        mSpaceDisplay.setLineColor(Color.AQUAMARINE);

        N = CaptureAudioDevice.SAMPLE_RATE/baud;

        //goertzelMark = new Goertzel(markHz, CaptureAudioDevice.SAMPLE_RATE);
        //goertzelMark.init();

        //goertzelSpace = new Goertzel(spaceHz, CaptureAudioDevice.SAMPLE_RATE);
        //goertzelSpace.init();

        convMark = new ConvolutionFreq(markHz, baud);
        convSpace = new ConvolutionFreq(spaceHz, baud);
        convPreamble = new ConvolutionPattern(generatePreamblePattern());

        buttonUpdate.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                markHz = Integer.parseInt(textFieldMark.getCharacters().toString());
                spaceHz = Integer.parseInt(textFieldSpace.getCharacters().toString());
                baud = Integer.parseInt(textFieldBaud.getCharacters().toString());

                labelResumeFreq.setText("Mark: "+markHz+" hz | Space: "+spaceHz+" hz | Baud: "+baud+"");

                N = CaptureAudioDevice.SAMPLE_RATE/baud;

                /*goertzelMark = new Goertzel(markHz, CaptureAudioDevice.SAMPLE_RATE);
                goertzelMark.init();

                goertzelSpace = new Goertzel(spaceHz, CaptureAudioDevice.SAMPLE_RATE);
                goertzelSpace.init();*/

                convMark = new ConvolutionFreq(markHz, baud);
                convSpace = new ConvolutionFreq(spaceHz, baud);
                convPreamble = new ConvolutionPattern(generatePreamblePattern());

            }
        });

        //goertzelOld = new GoertzelOld(CaptureAudioDevice.SAMPLE_RATE, 2200, N);
        //goertzelOld.initGoertzel();

    }

    @Override
    public void onStop() {

    }

    @Override
    public void processBuffer(short[] data) {

        //Normalizing Buffer to show Signal
        double[] normalBuffer = new double[data.length];
        for (int i = 0; i < data.length; i++) {

            normalBuffer[i] = (double)data[i]/(double)Short.MAX_VALUE;

        }
        mSignalDisplay.setData(normalBuffer);

        //Get the signal median
        double median = 0;
        for (int ii = 0; ii < normalBuffer.length; ii++) {
            median += normalBuffer[ii];
        }
        median = median/normalBuffer.length;

        //Find a preamble!
        /*double maxPreambleValue = 0;
        int probablePreambleIndex = -1;
        for (int i = 0; i < normalBuffer.length; i++) {

            double sum = convMark.convolutionSum(normalBuffer, i, median);
            if (sum > maxPreambleValue) {
                maxPreambleValue = sum;
                probablePreambleIndex = i;
            }
        }*/

        //Lets filter the Mark and show on mark display
        double[] markBuffer = new double[data.length];
        double[] normalMarkBuffer = new double[data.length];
        double maxMarkValue = 0;
        for (int i = 0; i < normalBuffer.length; i++) {

            double sum = convMark.convolutionSum(normalBuffer, i, median);
            markBuffer[i] = sum;
            if (sum > maxMarkValue) {
                maxMarkValue = sum;
            }
        }
        for (int i = 0; i < markBuffer.length; i++) {
            normalMarkBuffer[i] = (markBuffer[i]/maxMarkValue)*0.5d;
        }
        mMarkDisplay.setData(normalMarkBuffer);

        //Lets filter the Space and show on mark display
        double[] spaceBuffer = new double[data.length];
        double[] normalSpaceBuffer = new double[data.length];
        double maxSpaceValue = 0;
        for (int i = 0; i < normalBuffer.length; i++) {

            double sum = convSpace.convolutionSum(normalBuffer, i, median);
            spaceBuffer[i] = sum;
            if (sum > maxSpaceValue) {
                maxSpaceValue = sum;
            }

        }
        for (int i = 0; i < spaceBuffer.length; i++) {
            normalSpaceBuffer[i] = (spaceBuffer[i]/maxSpaceValue)*0.5d;
        }
        mSpaceDisplay.setData(normalSpaceBuffer);

        //Analyzing the bits
        String bitsBuffer = new String();
        for (int i = 0; i < data.length; i+=(int)symbolSize) {

            var markSum = 0;
            var spaceSum = 0;
            for (int ii = 0; ii < (int)symbolSize; ii++) {

                spaceSum += Math.abs(spaceBuffer[i+ii]);
                markSum += Math.abs(markBuffer[i+ii]);

            }
            if (markSum > spaceSum) {
                bitsBuffer += "1";
            } else {
                bitsBuffer += "0";
            }

        }

        //Se estivermos procurando um pacote, vamos comecar procurando preamble de sincronia
        int preambleindex = -1;
        if (currentState == STATE_PACKET_SEARCH) {

            for (int i = 0; i < bitsBuffer.length(); i++) {
                if (i + preamble.length() < bitsBuffer.length()) {
                    String preambleDetected = bitsBuffer.substring(i, i + preamble.length());
                    //Se acharmos o preamble, passamos para o proximo passo.
                    if (preambleDetected.equals(preamble)) {
                        textAreaMessage.setText("preamble:" + preambleDetected + "\n");
                        preambleindex = i;
                        currentState = STATE_PACKET_HEADER;
                        break;
                    }
                }
            }

        }

        //Se ja estivermos entrado na procura do char de inicio, vamos fazer a comparacão
        int headerIndex = -1;
        int headerBitsRest = 8;
        if (currentState == STATE_PACKET_HEADER) {

            System.out.println("Pacote possivelmente encontrado.");
            System.out.println("Descobrindo header da mensagem.");

            //Se estivermos no mesmo loop que encontrou o preamble, vamos tentar encaixar o buffer binario do header
            //se não houver espaço, esperaremos o próximo buffer.
            if (preambleindex != -1) {

                headerIndex = preambleindex + preamble.length();

                //O header é 1 char, ou seja, 8 bits
                //Vamos ver se cabe completo, senão adicionaremos apenas o que resta
                if (headerIndex + 8 < bitsBuffer.length()) {
                    for (int i = 0; i < 8; i++) {
                        binaryHeader += bitsBuffer.charAt(headerIndex+i);
                    }
                } else {
                    int rest = bitsBuffer.length() - headerIndex;
                    for (int i = 0; i < rest; i++) {
                        binaryHeader += bitsBuffer.charAt(headerIndex+i);
                    }
                }

            } else {

                //Se entramos aqui é porque o resto do header estava no próximo buffer
                headerIndex = 0;

                //Vamos verificar quantos bits ainda faltam do header
                headerBitsRest = 8 - binaryHeader.length();
                for (int i = 0; i < headerBitsRest; i++) {
                    binaryHeader += bitsBuffer.charAt(headerIndex+i);
                }

            }

            //Vamos verificar se o header está completo
            if (binaryHeader.length() == 8) {

                //Passamos para a proxima fase
                StringBuilder sb = new StringBuilder(); // Some place to store the chars
                Arrays.stream( // Create a Stream
                        binaryHeader.split("(?<=\\G.{8})") // Splits the input string into 8-char-sections (Since a char has 8 bits = 1 byte)
                ).forEach(s -> // Go through each 8-char-section...
                        sb.append((char) Integer.parseInt(s, 2)) // ...and turn it into an int and then to a char
                );
                String output = sb.toString();
                if (output.equals(headerChar)) {
                    textAreaMessage.appendText("header:" + output + "\n");

                    //NEXT
                    binaryHeader = "";
                    currentState = STATE_PACKET_MSGSIZE;

                } else {

                    //RESET
                    System.out.println("Packet Parse Error.");
                    textAreaMessage.setText("Packet Parse Error.");

                    binaryHeader = "";
                    currentState = STATE_PACKET_SEARCH;
                }

            }





            /*if (preambleindex >= 0 && preambleindex + 16 < bitsBuffer.length()) {
                String headerBinary = bitsBuffer.substring((preambleindex + preamble.length()), (preambleindex + preamble.length()) + 8);

                StringBuilder sb = new StringBuilder(); // Some place to store the chars
                Arrays.stream( // Create a Stream
                        headerBinary.split("(?<=\\G.{8})") // Splits the input string into 8-char-sections (Since a char has 8 bits = 1 byte)
                ).forEach(s -> // Go through each 8-char-section...
                        sb.append((char) Integer.parseInt(s, 2)) // ...and turn it into an int and then to a char
                );
                String output = sb.toString();
                textAreaMessage.appendText("header:" + output + "\n");

            } else {

                currentState = STATE_PACKET_HEADER;

            }*/

        }

        int msgSizeIndex = -1;
        int msgSizeBitsRest = 16;
        if (currentState == STATE_PACKET_MSGSIZE) {

            System.out.println("Descobrindo tamanho da mensagem.");

            //Se estivermos no mesmo loop que encontrou o header, vamos tentar encaixar o buffer binario do tamanho da mensagem
            //se não houver espaço, esperaremos o próximo buffer.
            if (headerIndex != -1) {

                msgSizeIndex = headerIndex + headerBitsRest;

                //O msgsize é um short, ou seja, 16 bits
                //Vamos ver se cabe completo, senão adicionaremos apenas o que resta
                if (msgSizeIndex + 16 < bitsBuffer.length()) {
                    for (int i = 0; i < 16; i++) {
                        binaryMsgSize += bitsBuffer.charAt(msgSizeIndex+i);
                    }
                } else {
                    int rest = bitsBuffer.length() - msgSizeIndex;
                    for (int i = 0; i < rest; i++) {
                        binaryMsgSize += bitsBuffer.charAt(msgSizeIndex+i);
                    }
                }

            } else {

                //Se entramos aqui é porque o resto do header estava no próximo buffer
                msgSizeIndex = 0;

                //Vamos verificar quantos bits ainda faltam do header
                msgSizeBitsRest = 16 - binaryMsgSize.length();
                for (int i = 0; i < msgSizeBitsRest; i++) {
                    binaryMsgSize += bitsBuffer.charAt(msgSizeIndex+i);
                }

            }

            //Vamos verificar se o header está completo
            if (binaryMsgSize.length() == 16) {

                //Tamanho da mensagem
                int intMsgSize = Integer.parseInt(binaryMsgSize, 2);
                if (intMsgSize > Short.MAX_VALUE || intMsgSize < 0) {

                    System.out.println("Packet Parse Error.");
                    textAreaMessage.setText("Packet Parse Error.");
                    //RESET
                    binaryMsgSize = "";
                    currentState = STATE_PACKET_SEARCH;

                } else {

                    msgSize = (short) Integer.parseInt(binaryMsgSize, 2);
                    textAreaMessage.appendText("msgsize:" + msgSize + "\n");

                    //NEXT
                    binaryMsgSize = "";
                    currentState = STATE_PACKET_MSGDECODING;
                }

            }

        }

        int msgDecodeIndex = -1;
        if (currentState == STATE_PACKET_MSGDECODING) {

            System.out.println("iniciando carregamento da mensagem.");

            if (msgSizeIndex != -1) {

                msgDecodeIndex = msgSizeIndex + msgSizeBitsRest;

            } else {

                msgDecodeIndex = 0;

            }

            int msgRest = (msgSize * 8) - binaryMsg.length();
            System.out.println("index da mensagem:"+msgDecodeIndex+" - ainda faltam:"+msgRest+" bits.");
            //Se a mensagem estiver completa dentro desse buffer, vamos pegar tudo
            if (msgDecodeIndex + msgRest < bitsBuffer.length()) {

                System.out.println("carregando final da mensagem.");
                for (int i = msgDecodeIndex; i < msgDecodeIndex + msgRest; i++) {

                    binaryMsg += bitsBuffer.charAt(i);

                }

                //Acabamos de decodificar, vamos printar a mensagem.
                StringBuilder sb = new StringBuilder(); // Some place to store the chars
                Arrays.stream( // Create a Stream
                        binaryMsg.split("(?<=\\G.{8})") // Splits the input string into 8-char-sections (Since a char has 8 bits = 1 byte)
                ).forEach(s -> // Go through each 8-char-section...
                        sb.append((char) Integer.parseInt(s, 2)) // ...and turn it into an int and then to a char
                );
                String output = sb.toString();
                textAreaMessage.setText(output);

                //RESET
                binaryMsg = "";
                msgSize = 0;
                currentState = STATE_PACKET_SEARCH;

            } else {

                System.out.println("carregando parte da mensagem");
                //Senão vamos pegar apenas parte
                for (int i = msgDecodeIndex; i < bitsBuffer.length(); i++) {

                    binaryMsg += bitsBuffer.charAt(i);

                }
            }
        }

        /*if (probablePreambleIndex/(int)symbolSize + preamble.length() < bitsBuffer.length()) {
            String preambleDetected = bitsBuffer.substring(probablePreambleIndex / (int) symbolSize, probablePreambleIndex / (int) symbolSize + preamble.length());
            if (preambleDetected.equals(preamble)) {
                textAreaMessage.appendText("preamble:" + preambleDetected);
            }
        }*/

        /*StringBuilder sb = new StringBuilder(); // Some place to store the chars
        Arrays.stream( // Create a Stream
                bitsBuffer.split("(?<=\\G.{8})") // Splits the input string into 8-char-sections (Since a char has 8 bits = 1 byte)
        ).forEach(s -> // Go through each 8-char-section...
                sb.append((char) Integer.parseInt(s, 2)) // ...and turn it into an int and then to a char
        );
        String output = sb.toString();

        textAreaMessage.appendText(output);*/

        /*int samples = data.length/N;

        for (int i = 0; i < data.length-N; i+= N) {

            //MARK ================================
            double mark_sum_sqr = 0;

            for (int ii = 0; ii < N; ii++) {

                goertzelMark.processSample((float)data[i+ii]);
                mark_sum_sqr += (double)data[i+ii] * (double)data[i+ii];

            }

            float mark_power = (float)mark_sum_sqr / (float)N;
            float mark_rms = (float) Math.sqrt(mark_power);

            float mark_magnitude = (float)Math.sqrt(goertzelMark.getMagnitude());
            float mark_correlation = mark_magnitude/mark_rms;

            //System.out.printf("dbs_mark:%f\n", mark_correlation);
            goertzelMark.reset();

            //SPACE ===============================
            double space_sum_sqr = 0;

            for (int jj = 0; jj < N; jj++) {

                goertzelSpace.processSample((float)data[i+jj]);
                space_sum_sqr += (double)data[i+jj] * (double)data[i+jj];

            }

            float space_power = (float)space_sum_sqr / (float)N;
            float space_rms = (float) Math.sqrt(space_power);

            float space_magnitude = (float)Math.sqrt(goertzelSpace.getMagnitude());
            float space_correlation = space_magnitude/space_rms;

            //System.out.printf("dbs_space:%f\n", space_correlation);

            //Verification ========================
            if (space_correlation > 11 || mark_correlation > 11) {

                if (space_correlation > mark_correlation) {
                    textAreaMessage.setText(textAreaMessage.getText()+"0");
                } else {
                    textAreaMessage.setText(textAreaMessage.getText()+"1");
                }

            }

            goertzelSpace.reset();

        }

        // System.out.println("samples: "+samples);
        // System.out.println("detections: "+detections);*/

    }

    private double[] generatePreamblePattern() {

        int duration = (int)((float)OutputAudioDevice.SAMPLE_RATE/(float)baud);
        double[] preamblePatt = new double[duration*preamble.length()];

        for (int i = 0; i < preamblePatt.length; i+= duration) {

            if (preamble.charAt(i/duration) == '0') {

                for (int ii = 0; ii < duration; ii++) {
                    preamblePatt[i+ii] = generateSineWavefreq(spaceHz, ii);
                }

            } else if (preamble.charAt(i/duration) == '1') {

                for (int ii = 0; ii < duration; ii++) {
                    preamblePatt[i+ii] = generateSineWavefreq(markHz, ii);
                }

            }
        }

        return preamblePatt;
    }

    private double generateSineWavefreq(float frequencyOfSignal, float index) {

        float samplingInterval = (float) ((float)CaptureAudioDevice.SAMPLE_RATE / (float)frequencyOfSignal);
        float angle = ((2.0f * (float)Math.PI * (float)index / samplingInterval));

        double result = (Math.sin(angle) * 1d);

        return result;

    }

}
