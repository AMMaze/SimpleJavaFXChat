package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    Controller c;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();
        Parent root = loader.load(getClass().getResourceAsStream("/fxml/sample.fxml"));
        c = loader.getController();

        c.ctxMenu = new ContextMenu();
        MenuItem item1 = new MenuItem("Отправить сообщение");

        MenuItem item2 = new MenuItem("Чёрный список");

        item1.setOnAction(actionEvent -> c.sendMsgDialog());
        item2.setOnAction(actionEvent -> {
            try {
                c.out.writeUTF("/blacklist " + c.clientList.getSelectionModel().getSelectedItem());
            } catch (IOException ignore) {}
        });

        c.ctxMenu.getItems().addAll(item1, item2);

        primaryStage.setTitle("Chat 2k19");
        Scene scene = new Scene(root, 350, 350);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            c.Dispose();
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
