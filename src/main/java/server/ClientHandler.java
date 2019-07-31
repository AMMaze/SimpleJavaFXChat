package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Main server;
    private String nick;
    List<String> blackList;

    public String getNick() {
        return nick;
    }

    public boolean checkBlackList(String nick) {
        return blackList.contains(nick);
    }

    public ClientHandler(Socket socket, Main server) {
        try {
            this.blackList = new ArrayList<>();
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/auth")) {
                                String[] tokens = str.split(" ");
                                String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                                if (newNick != null) {
                                    if (!server.isNickBusy(newNick)) {
                                        sendMsg("/authok " + newNick);
                                        nick = newNick;
                                        server.subscribe(ClientHandler.this);
                                        blackList = BlacklistService.getBlacklist(newNick);
                                        break;
                                    } else {
                                        sendMsg("Учетная запись уже используется");
                                    }
                                } else {
                                    sendMsg("Неверный логин/пароль");
                                }
                            } else if (str.startsWith("/reg")) {
                                String[] tokens = str.split(" ");
                                String newNick = AuthService.addNewUser(tokens[1], tokens[2], tokens[3]);
                                if (newNick != null) {
                                    sendMsg("/authok " + newNick);
                                    nick = newNick;
                                    server.subscribe(ClientHandler.this);
                                    break;
                                } else {
                                    sendMsg("Данный логин/никнейм уже занят");
                                }
                            }
//                            server.broadcastMsg(ClientHandler.this, str);
                        }

                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.equals("/end")) {
                                    out.writeUTF("/serverclosed");
                                    break;
                                }
                                if (str.startsWith("/w ")) {
                                    String[] tokens = str.split(" ", 3);
                                    server.sendPersonalMsg(ClientHandler.this, tokens[1], tokens[2]);
                                }
                                if (str.startsWith("/blacklist ")) {
                                    String[] tokens = str.split(" ");
                                    if (!blackList.contains(tokens[1])) {
                                        blackList.add(tokens[1]);
                                        BlacklistService.addToBlacklist(nick, tokens[1]);
                                        sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список");
                                    } else {
                                        blackList.remove(tokens[1]);
                                        BlacklistService.removeFromBlacklist(nick, tokens[1]);
                                        sendMsg("Вы удалили пользователя " + tokens[1] + " из черного списка");
                                    }
                                }
                            } else {
                                server.broadcastMsg(ClientHandler.this, nick + ": " + str);
                            }
                        }
                    } catch (EOFException e) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    server.unsubscribe(ClientHandler.this);
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
