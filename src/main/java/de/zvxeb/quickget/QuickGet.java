/**
 QuickGet
 Copyright 2022 Hendrik Iben

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package de.zvxeb.quickget;

import fi.iki.elonen.NanoHTTPD;
import io.nayuki.qrcodegen.QrCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class QuickGet implements Runnable {
    Logger log = LoggerFactory.getLogger(QuickGet.class);

    static final String MIME_OCTET_STREAM = "application/octet-stream";

    static final String MIME_GIF = "image/gif";
    static final String MIME_JPG = "image/jpg";
    static final String MIME_PNG = "image/png";

    JFrame frame;

    JButton serverButton;

    JTextArea textArea_Log;
    JTextField textField_Status;

    JTextField textField_ExternalUrl;
    JTextField textField_ServerPort;

    JFileChooser fileChooser_Input;
    File selectedFile = null;
    JLabel label_InputFile;
    JTextField textField_InputFileName;
    JComboBox<String> comboxBox_InputFileMime;
    DefaultComboBoxModel<String> mimeModel;

    String codeUri = "";
    ImageScaler codeScaler;

    ServerThread serverThread = null;

    public static short i8u(byte b) {
        return (short)(((short)b)&0xff);
    }

    public static String formatIPv4Address(Inet4Address addr) {
        ByteBuffer bb = ByteBuffer.wrap(addr.getAddress()).order(ByteOrder.BIG_ENDIAN);
        return String.format("%d.%d.%d.%d", i8u(bb.get()), i8u(bb.get()), i8u(bb.get()), i8u(bb.get()));
    }

    public static String formatIPv6Address(Inet6Address addr) {
        ByteBuffer bb = ByteBuffer.wrap(addr.getAddress()).order(ByteOrder.BIG_ENDIAN);
        return String.format(
            "%x:%x:%x:%x:%x:%x:%x:%x",
            bb.getShort(), bb.getShort(), bb.getShort(), bb.getShort(),
            bb.getShort(), bb.getShort(), bb.getShort(), bb.getShort()
        );
    }

    public void run() {
        frame = new JFrame("QuickGet", MouseInfo.getPointerInfo().getDevice().getDefaultConfiguration());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationByPlatform(true);

        createUI();

        try {
            for (Iterator<NetworkInterface> it = NetworkInterface.getNetworkInterfaces().asIterator(); it.hasNext(); ) {
                NetworkInterface nic = it.next();
                for(InterfaceAddress ia : nic.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if(addr instanceof Inet4Address) {
                        addLog("IPv4-Interface: " + formatIPv4Address((Inet4Address) addr));
                    }
                    if(addr instanceof Inet6Address) {
                        addLog("IPv6-Interface: " + formatIPv6Address((Inet6Address) addr));
                    }
                }
            }
        } catch (SocketException e) {
            addLogError("Could not fetch network interfaces...");
        }

        frame.pack();
        frame.setSize(new Dimension(400, 400));
        frame.setVisible(true);

        String file = getSetting("file", "QG_FILE", "");
        if(!emptyString(file)) {
            selectFile(new File(file.trim()));
        }

        String name = getSetting("name", "QG_NAME", "");
        if(!emptyString(name)) {
            textField_InputFileName.setText(name.trim());
            updateServer();
        }

        if(Boolean.parseBoolean(getSetting("start", "QG_START", "false"))) {
            startServer();
        }

        addLog("Ready...");
    }

    public static boolean emptyString(String s) {
        if(s==null) return true;
        if(s.trim().length()==0) return true;
        return false;
    }

    public static String getSetting(String prop, String env, String defaultSetting) {
        String tmp = System.getProperty(prop);
        if(!emptyString(tmp)) {
            return tmp.trim();
        }

        tmp = System.getenv(env);
        if(!emptyString(tmp)) {
            return tmp.trim();
        }

        return defaultSetting;
    }

    private void startServer() {
        int port = parsePort(textField_ServerPort.getText());
        if(selectedFile != null) {
            if(port > 1) {
                serverThread = new ServerThread(port, selectedFile, textField_InputFileName.getText(), comboxBox_InputFileMime.getSelectedItem().toString());
                try {
                    addLog("Starting server...");
                    updateServer(); // for log
                    serverButton.setText("Stop");
                    serverThread.start();
                    evaluateCode();
                } catch (IOException ioException) {
                    log.error("Error", ioException);
                }
            } else {
                addLogError(String.format("Invalid port: %s", textField_ServerPort.getText()));
            }
        }
    }

    private JComponent createServerSettings() {
        String serverUrl = getSetting("url", "QG_URL", "http://localhost");

        JPanel serverSettings = new JPanel();
        serverSettings.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(5,5,0,4);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        serverSettings.add(new JLabel("External URL"), gbc);

        gbc.gridx++;
        gbc.gridwidth=3;
        gbc.weightx = 1;
        gbc.insets = new Insets(5,5,0,16);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        serverSettings.add(textField_ExternalUrl = new JTextField(serverUrl, 16), gbc);

        gbc.gridwidth=1;

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.insets = new Insets(5,5,0,4);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        serverSettings.add(serverButton = new JButton(new AbstractAction("Start") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(serverThread != null) {
                    addLog("Stopping server...");
                    serverButton.setText("Start");
                    serverThread.stop();
                    serverThread = null;
                    return;
                }
                ((JButton)e.getSource()).setText("Stop");
                startServer();
            }
        }), gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        serverSettings.add(new JLabel("Port"), gbc);

        String port = getSetting("port", "QG_PORT", "80");
        textField_ServerPort = new JTextField(port, 5);
        gbc.gridx++;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        serverSettings.add(textField_ServerPort, gbc);

        // Textfield for port will not scale nicely without this...
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        serverSettings.add(new JPanel(), gbc);

        return serverSettings;
    }

    private void setStatus(String message) {
        textField_Status.setForeground(frame.getForeground());
        textField_Status.setText(message);
    }

    private void setStatusError(String errorMessage) {
        textField_Status.setForeground(Color.red);
        textField_Status.setText(errorMessage);
    }

    private void addLog(String message) {
        log.info("Log-Message: {}", message);
        textArea_Log.setText(textArea_Log.getText()+message+"\n");
        setStatus(message);
    }

    private void addLogError(String errorMessage) {
        log.error("Log-Message-Error: {}", errorMessage);
        textArea_Log.setText(textArea_Log.getText()+errorMessage+"\n");
        setStatusError(errorMessage);
    }

    private void selectFile(File f) {
        selectedFile = f;
        if(f!=null) {
            addLog(String.format("Selected %s", selectedFile.getAbsolutePath()));
            label_InputFile.setText(selectedFile.getName());
            label_InputFile.setToolTipText(selectedFile.getAbsolutePath());
            textField_InputFileName.setText(selectedFile.getName());
        }
        updateServer();
    }

    private void updateServer() {
        if(serverThread!=null && selectedFile != null) {
            serverThread.setFile(selectedFile, textField_InputFileName.getText(), (String)comboxBox_InputFileMime.getSelectedItem());
            addLog(String.format("Serving %s as %s (%s)", serverThread.fileToServe.getName(), serverThread.nameToServe, serverThread.mimeTypeToServe));
            evaluateCode();
        }
    }

    private JComponent createFilePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(5,5,0,4);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        panel.add(new JButton(new AbstractAction("Select File") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(fileChooser_Input == null) {
                    fileChooser_Input = new JFileChooser();
                    fileChooser_Input.setAcceptAllFileFilterUsed(true);
                    fileChooser_Input.setFileSelectionMode(JFileChooser.FILES_ONLY);
                }
                if(fileChooser_Input.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    selectFile(fileChooser_Input.getSelectedFile());
                }
            }
        }), gbc);

        label_InputFile = new JLabel("<<no file selected...>>");

        gbc.gridx++;
        gbc.weightx = 0;
        panel.add(label_InputFile, gbc);

        gbc.gridy++;
        gbc.gridx=0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("Name"), gbc);


        gbc.gridx++;
        gbc.weightx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(textField_InputFileName = new JTextField(16), gbc);
        textField_InputFileName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFileName();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFileName();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFileName();
            }

            public void updateFileName() {
                updateServer();
            }
        });

        gbc.gridx++;
        gbc.weightx = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JPanel(), gbc);

        gbc.gridy++;
        gbc.gridx=0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel("MIME"), gbc);

        gbc.gridx++;
        gbc.weightx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        List<String> mimes = new LinkedList<>();
        mimes.addAll(Arrays.asList(MIME_OCTET_STREAM, MIME_GIF, MIME_JPG, MIME_PNG));

        String extraMimeSetting = getSetting("extraMimes", "QG_EXTRA_MIMES", "");

        List<String> extraMimeList = Arrays.stream(extraMimeSetting.split(",")).filter(s -> !emptyString(s)).collect(Collectors.toList());

        for(String m : extraMimeList) {
            if(!mimes.contains(m)) {
                mimes.add(m);
            }
        }

        String selectedMime = getSetting("mime", "QG_MIME", MIME_OCTET_STREAM);

        if(!mimes.contains(selectedMime)) {
            mimes.add(selectedMime);
        }

        // get actual object
        for(String m : mimes) {
            if(m.equals(selectedMime)) {
                selectedMime = m;
                break;
            }
        }

        mimeModel = new DefaultComboBoxModel<>(mimes.toArray(new String [mimes.size()]));
        mimeModel.setSelectedItem(selectedMime);
        panel.add(comboxBox_InputFileMime = new JComboBox<>(mimeModel), gbc);
        comboxBox_InputFileMime.setEditable(true);
        comboxBox_InputFileMime.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    updateServer();
                }
            }
        });

        updateServer();

        gbc.gridx++;
        gbc.weightx = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private void updateCode(String code) {
        if(!codeUri.equals(code)) {
            codeUri = code;
            codeScaler.setImage(Util.getImage(QrCode.encodeText(code, QrCode.Ecc.LOW), null, null));
        }
    }

    private void evaluateCode() {
        String file = textField_InputFileName.getText();
        String url = textField_ExternalUrl.getText();

        if(!url.endsWith("/")) {
            url += "/";
        }

        updateCode(url + file);
    }

    private void createUI() {
        Container con = new JPanel();

        con.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        textArea_Log = new JTextArea();
        textArea_Log.setEditable(false);
        textArea_Log.setBackground(SystemColor.control);
        JScrollPane scrollPane = new JScrollPane(textArea_Log, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        textField_Status = new JTextField("Status");
        textField_Status.setEditable(false);
        textField_Status.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        con.add(createServerSettings(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        con.add(createFilePanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel codePanel = new JPanel();
        codePanel.setLayout(new BorderLayout());
        codePanel.setBorder(BorderFactory.createTitledBorder("Code"));
        codePanel.add(codeScaler = new ImageScaler(Util.getImage(QrCode.encodeText(codeUri, QrCode.Ecc.LOW), null, null)), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        codePanel.setMinimumSize(new Dimension(100, 100));
        splitPane.setResizeWeight(0.65);
        splitPane.setTopComponent(codePanel);
        splitPane.setBottomComponent(scrollPane);
        con.add(splitPane, gbc);

        gbc.gridy++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        con.add(textField_Status, gbc);

        frame.setLayout(new BorderLayout());
        frame.add(con, BorderLayout.CENTER);
    }

    public static class ServerThread extends NanoHTTPD {

        private Logger log = LoggerFactory.getLogger(ServerThread.class);

        private File fileToServe;
        private String nameToServe;
        private String mimeTypeToServe;

        public ServerThread(int port, File fileToServe, String nameToServe, String mimeTypeToServe) {
            super(port);
            setFile(fileToServe, nameToServe, mimeTypeToServe);
        }

       public void setFile(File fileToServe, String nameToServe, String mimeTypeToServe) {
           this.fileToServe = fileToServe;
           this.nameToServe = nameToServe == null ? fileToServe.getName() : nameToServe;
           this.mimeTypeToServe = mimeTypeToServe == null ? MIME_OCTET_STREAM : mimeTypeToServe;
       }

        @Override
        public Response serve(IHTTPSession session) {
            if(!session.getMethod().equals(Method.GET)) {
                return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed!");
            }
            String requestUri = session.getUri();

            if(requestUri.startsWith("/")) {
                requestUri = requestUri.substring(1);
            }

            if(nameToServe.equalsIgnoreCase(requestUri)) {
                try {
                    FileInputStream fis = new FileInputStream(fileToServe);
                    if(fileToServe.length() > 1024*64) {
                        log.debug("Chunking transfer...");
                        return newChunkedResponse(Response.Status.OK, mimeTypeToServe, fis);
                    } else {
                        log.debug("Fixed transfer...");
                        return newFixedLengthResponse(Response.Status.OK, mimeTypeToServe, fis, fileToServe.length());
                    }
                } catch (FileNotFoundException e) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found!");
                }
            }

            return super.serve(session);
        }
    }

    public static int parsePort(String portString) {
        try {
            int port = Integer.parseInt(portString);
            if(port < 1 || port > 65536) return -2;
            return port;
        } catch(NumberFormatException nfe) {
            return -1;
        }
    }

    public static class ImageScaler extends JPanel {
        private BufferedImage image;

        public ImageScaler(BufferedImage image) {
            setImage(image);
            setMaximumSize(new Dimension(1024,1024));
        }
        public ImageScaler() {
            this(null);
        }

        public void setImage(BufferedImage image) {
            this.image = image;
            if(image!=null) {
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            } else {
                setPreferredSize(new Dimension(16,16));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int canvasWidth = getWidth();
            int canvasHeight = getHeight();
            Graphics2D g2d = (Graphics2D)g;
            g.clearRect(0,0,getWidth(),getHeight());
            if(image!=null) {
                int width = image.getWidth();
                int height = image.getHeight();

                int maxScaleW = Math.max(1, canvasWidth / width);
                int maxScaleH = Math.max(1, canvasHeight / height);
                int scale = Math.min(maxScaleW, maxScaleH);

                int offsetX = (canvasWidth - (scale * width)) / 2;
                int offsetY = (canvasHeight - (scale *height)) / 2;

                BufferedImageOp scalerOp = new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

                g2d.drawImage(image, scalerOp, offsetX, offsetY);
            }
        }
    }

    public static void main(String...args) {
        SwingUtilities.invokeLater(new QuickGet());
    }
}
