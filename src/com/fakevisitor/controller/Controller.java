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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Random;
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

    //http://visitorcounter1.000webhostapp.com/ - test site
    private Service<Void> visitorService;
    private Service<Void> createVisitorService(){
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
                            Platform.runLater(() -> {
                                try {
                                    loadNewTab(finalI);
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                } finally {
                                    latch.countDown();
                                }
                            });
                            latch.await();
                            //background work
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

    public void loadNewTab(int pageNumber) throws IOException, InterruptedException {
        double progressTick = 1/Double.parseDouble(repetitionsInput.getText());
        CookieManager manager = new java.net.CookieManager();
        CookieHandler.setDefault(manager);
        Tab tab = new Tab();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load(urlInput.getText().equals("") ? URL : urlInput.getText());
        goToRandomPage(urlInput.getText(), webEngine, pageNumber+1, tab);
        tab.setContent(webView);
        if (pageNumber > 0)
            tabPane.getTabs().remove(0);
        tabPane.getTabs().add(tab);
        manager.getCookieStore().removeAll();
        progressLabel.setText(pageNumber+1 + "/" + Integer.parseInt(repetitionsInput.getText()));
        progressBar.setProgress(progressBar.getProgress() + progressTick);
        if (pageNumber == Integer.parseInt(repetitionsInput.getText())-1){
            progressBar.setProgress(1);
            progressBar.setStyle("-fx-accent: green");
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
        }
    }

    public void goToRandomPage(String url, WebEngine webEngine, int pageNumber, Tab tab) throws IOException {
        ArrayList<String> links = new ArrayList<>();
        Document doc = Jsoup.connect(url).get();
        Elements elements = doc.select("a[href]"); // a with href
        for (Element element : elements) {
            links.add(element.attr("href"));
        }
        String[] urlParts = url.split("/");
        StringBuilder rootUrl = new StringBuilder();
        if (urlParts.length > 2)
            for (int i = 0; i < 3; i++){
                rootUrl.append(urlParts[i]).append("/");
            }
        else
            rootUrl.append(url);
        links.removeIf(z -> z.equals("#") || z.equals("index.html") || !z.endsWith(".html"));
        if (links.size() > 0) {
            String newUrl = rootUrl + links.get(new Random().nextInt(links.size() - 1));
            webEngine.load(newUrl);
            tab.setText(pageNumber + newUrl.replaceAll(".html", ""));
        }
        else
            tab.setText(Integer.toString(pageNumber));
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
    public void showBrowserHandler(){
        tabPane.setVisible(showBrowserCb.isSelected());
    }
}
