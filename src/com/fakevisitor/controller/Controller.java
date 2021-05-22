package com.fakevisitor.controller;

import com.fakevisitor.Toast;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;

public class Controller {
    public static String URL = "https://www.google.ru/";

    @FXML
    private TextField urlInput;
    @FXML
    private TextField repetitionsInput;
    @FXML
    private TextField delayInput;
    @FXML
    private CheckBox showBrowserCb;
    @FXML
    private Button startBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private TabPane tabPane;
    @FXML
    private Label finishTimer;
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar progressBar;

    @FXML private AnchorPane ap;

    @FXML
    public void initialize(){
        stopBtn.setDisable(true);
        progressBar.setProgress(0);
        visitorService = createVisitorService();
        progressLabel.setText(0 + "/" + Integer.parseInt(repetitionsInput.getText()));
        repetitionsInput.textProperty().addListener(
                (ov, oldValue, newValue) ->
                {
                    try {
                        progressLabel.setText(0 + "/" + Integer.parseInt(repetitionsInput.getText()));
                    } catch (NumberFormatException e){
                        progressLabel.setText(0 + "/" + 0);
                    }
                }
        );
    }

    //http://visitorcounter1.000webhostapp.com/
    private Service<Void> visitorService;

    private Service<Void> createVisitorService(){
        double progressTick = 1/Double.parseDouble(repetitionsInput.getText());
        return new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        //Background work
                        final CountDownLatch latch = new CountDownLatch(1);
                        for (int i = 0; i < Integer.parseInt(repetitionsInput.getText()); i++) {
                            int finalI = i;
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        //FX Stuff done here
                                        java.net.CookieManager manager = new java.net.CookieManager();
                                        java.net.CookieHandler.setDefault(manager);
                                        Tab tab = new Tab();
                                        tab.setText("" + (finalI + 1));
                                        WebView webView = new WebView();
                                        WebEngine webEngine = webView.getEngine();
                                        webEngine.load(urlInput.getText().equals("") ? URL : urlInput.getText());
                                        tab.setContent(webView);
                                        tabPane.getTabs().add(tab);
                                        manager.getCookieStore().removeAll();
                                        progressLabel.setText(finalI+1 + "/" + Integer.parseInt(repetitionsInput.getText()));
                                        progressBar.setProgress(progressBar.getProgress() + progressTick);
                                        if (finalI == Integer.parseInt(repetitionsInput.getText())-1){
                                            progressBar.setProgress(1);
                                            progressBar.setStyle("-fx-accent: green");
                                            startBtn.setDisable(false);
                                        }
                                    } finally {
                                        latch.countDown();
                                    }
                                }
                            });
                            latch.await();
                            //Keep with the background work
                            try {
                                Thread.sleep(Integer.parseInt(delayInput.getText()) * 1000L);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return null;
                            }
                        }
                        return null;
                    }
                };
            }
        };
    }

    @FXML
    public void onStartClick() {
        if (repetitionsInput.getText().isEmpty() || delayInput.getText().isEmpty()) {
            Toast.makeText((Stage)ap.getScene().getWindow(), "Fill in all the fields!", 1000, 500, 500);
            return;
        }
        tabPane.getTabs().clear();
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        int seconds = LocalTime.now().getHour()*3600 +
                LocalTime.now().getMinute()*60 +
                LocalTime.now().getSecond() +
                (Integer.parseInt(repetitionsInput.getText())-1)*Integer.parseInt(delayInput.getText());
        finishTimer.setText(""+LocalTime.of(seconds % (3600*24) / 3600, (seconds % 3600)/60%60, seconds % 60));
        visitorService.restart();
    }

    @FXML
    public void onStopClick(){
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        visitorService.cancel();
        finishTimer.setText(""+LocalTime.of(0, 0)+":00");
        progressBar.setProgress(0);
    }

    @FXML
    public void onShowBrowserClick(){
        tabPane.setVisible(showBrowserCb.isSelected());
    }
}
