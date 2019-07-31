package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;


public class Controller {
//    @FXML
//    TextArea textArea;

    @FXML
    TextField textField;

    @FXML
    Button btn1;

    @FXML
    HBox bottomPanel;

    @FXML
    HBox upperPanel;

    @FXML
    TextField loginField;

    @FXML
    TextField passwordField;

    @FXML
    ListView<String> clientList;

    @FXML
    ScrollPane scrollPane;

    @FXML
    VBox msgList;

    @FXML
    TextField regLoginField;

    @FXML
    TextField regPasswordField;

    @FXML
    TextField regNickField;

    @FXML
    HBox regPanel;

    @FXML
    HBox sysMsg;

    @FXML
    Label sysTxt;


    private boolean isAuthorized;

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if(!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            clientList.setVisible(false);
            clientList.setManaged(false);
            regPanel.setVisible(true);
            regPanel.setManaged(true);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientList.setVisible(true);
            clientList.setManaged(true);
            regPanel.setVisible(false);
            regPanel.setManaged(false);
        }
    }

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    final String IP_ADDRESS = "localhost";
    final int PORT = 8189;

    private String nick = "Server_Message";
    private String authString;
    private volatile boolean reconnect = false;

    public void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/authok")) {
                                setAuthorized(true);
                                nick = str.split(" ")[1];
                                Platform.runLater(() -> msgList.getChildren().clear());
                                break;
                            } else {
                                addMsg(str);
                            }
                        }

                        while (true) {
                            String str;
                            try {
                                str = in.readUTF();
                            } catch (EOFException e) {
                                showSysMessage("Потеряно соединение с сервером");
                                tryToReconnect();
                                hideSysMessage();
                                continue;
                            }
                            if (str.equals("/serverclosed")) break;
                            if (str.startsWith("/clientlist")) {
                                String[] tokens = str.split(" ");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        clientList.getItems().clear();
                                        for (int i = 1; i < tokens.length; i++) {
                                            clientList.getItems().add(nick.equals(tokens[i]) ? "Вы: " + tokens[i] : tokens[i]);
                                        }
                                    }
                                });
                            } else {
                                addMsg(str);
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(false);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Dispose() {
        System.out.println("Отправляем сообщение о закрытии");
        try {
            if(out != null) {
                out.writeUTF("/end");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            if (!reconnect)
                out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if(socket == null || socket.isClosed()) {
            connect();
        }
        try {
            authString = "/auth " + loginField.getText() + " " + passwordField.getText();
            out.writeUTF(authString);
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToReg(ActionEvent actionEvent) {
        if(socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/reg " + regLoginField.getText() + " " + regNickField.getText() + " " + regPasswordField.getText());
            regLoginField.clear();
            regPasswordField.clear();
            regNickField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ContextMenu ctxMenu;

    public void selectClient(MouseEvent mouseEvent) {
        if(mouseEvent.getClickCount() == 2) {
//            System.out.println("Двойной клик");
            sendMsgDialog();
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
//            ContextMenu contextMenu = new ContextMenu();
//            MenuItem item1 = new MenuItem("Отправить сообщение");
//
//            MenuItem item2 = new MenuItem("Чёрный список");
//
//            item1.setOnAction(actionEvent -> sendMsgDialog());
//            item2.setOnAction(actionEvent -> {
//                try {
//                    out.writeUTF("/blacklist " + clientList.getSelectionModel().getSelectedItem());
//                } catch (IOException ignore) {}
//            });
//
//            contextMenu.getItems().addAll(item1, item2);
            ctxMenu.show(clientList, mouseEvent.getScreenX(), mouseEvent.getScreenY());
        }
    }

    public void sendMsgDialog() {
        Platform.runLater(() -> {
            TextInputDialog inputDialog = new TextInputDialog();

            String name = clientList.getSelectionModel().getSelectedItem();

            inputDialog.setTitle("Личное сообщение");
            inputDialog.setHeaderText("Отправить сообщение " + name + ":");
            inputDialog.setContentText("Сообщение: ");

            String res = inputDialog.showAndWait().orElse(null);
            if (res != null) {
                try {
                    out.writeUTF("/w " + name + " " + res);
                } catch (IOException ignore) {}
            }
        });
    };

    public boolean ifMyMsg(String msg) {
        return msg.startsWith(nick);
    }

    public void addMsg(String str) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                HBox hBox = new HBox();
                Label msg = new Label();
                msg.setWrapText(true);
                hBox.getChildren().add(msg);
                hBox.setStyle("-fx-padding: 1;");
                msg.setPadding(new Insets(5, 5, 5, 5));
                msg.setMaxWidth(scrollPane.getWidth() * 0.7);
                if (ifMyMsg(str)) {
                    msg.setText(str.split(" ", 2)[1]);
                    hBox.setAlignment(Pos.CENTER_RIGHT);
                    msg.setStyle("-fx-text-fill: black;" +
                            "-fx-background-color: white");
                } else {
                    msg.setText(str);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    msg.setStyle("-fx-text-fill: white;" +
                            "-fx-background-color: blue");
                }
                msgList.getChildren().add(hBox);
                msgList.heightProperty().addListener(observable -> scrollPane.setVvalue(1D));
                scrollPane.widthProperty().addListener((observable -> msg.setMaxWidth(scrollPane.getWidth() * 0.7)));
            }
        });
    }

    public void showSysMessage(String msg) {
        Platform.runLater(() -> {
            sysMsg.setVisible(true);
            sysMsg.setManaged(true);
            sysTxt.setText(msg);
        });
    }

    public void hideSysMessage() {
        Platform.runLater(() -> {
            sysMsg.setVisible(false);
            sysMsg.setManaged(false);
        });
    }

    public void tryToReconnect() {
        reconnect = true;
        while(reconnect) {
            try {
                socket = new Socket(IP_ADDRESS, PORT);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(authString);
                System.out.println(authString);
                while(true) {
                    String str = in.readUTF();
                    if (str.startsWith("/authok")) {
                        reconnect = false;
                        break;
                    }
                }
            } catch (IOException ignore) {}

            try {
                Thread.sleep(500);
            } catch (InterruptedException e ){
                e.printStackTrace();
            }
        }
    }
}
