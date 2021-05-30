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
    private TextField visitsNumberInput;
    @FXML
    private TextField visitDurationInput;
    @FXML
    private CheckBox showBrowserCb;
    @FXML
    private CheckBox goToRandomPageCb;
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

    @FXML private AnchorPane ap;

    @FXML
    public void initialize(){
        stopBtn.setDisable(true);
        visitorService = createVisitorService();
        progressLabel.setText(0 + "/" + Integer.parseInt(visitsNumberInput.getText()));
        visitsNumberInput.textProperty().addListener(
                (ov, oldValue, newValue) ->
                {
                    try {
                        progressLabel.setText(0 + "/" + Integer.parseInt(visitsNumberInput.getText()));
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
                        for (int i = 0; i < Integer.parseInt(visitsNumberInput.getText()); i++) {
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
                                Thread.sleep(Integer.parseInt(visitDurationInput.getText()) * 1000L);
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
        CookieManager manager = new java.net.CookieManager();
        CookieHandler.setDefault(manager);
        Tab tab = new Tab();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load(urlInput.getText().equals("") ? URL : urlInput.getText());
        if (goToRandomPageCb.isSelected())
            goToRandomPage(urlInput.getText(), webEngine, tab);
        tab.setContent(webView);
        if (pageNumber > 0)
            tabPane.getTabs().remove(0);
        tabPane.getTabs().add(tab);
        manager.getCookieStore().removeAll();
        progressLabel.setText(pageNumber+1 + "/" + Integer.parseInt(visitsNumberInput.getText()));
        if (pageNumber == Integer.parseInt(visitsNumberInput.getText())-1){
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
        }
    }

    public void goToRandomPage(String url, WebEngine webEngine, Tab tab) throws IOException {
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
            tab.setText(newUrl
                    .replaceAll("\\.html", "")
                    .replaceAll("http://", "")
                    .replaceAll("https://", "")
                    .replaceAll("www\\.", ""));
        }
        else
            tab.setText(urlInput.getText()
                    .replaceAll("http://", "")
                    .replaceAll("https://", "")
                    .replaceAll("www\\.", ""));
    }

    public void changeFieldsState(boolean state){
        startBtn.setDisable(state);
        stopBtn.setDisable(!state);
        urlInput.setDisable(state);
        visitsNumberInput.setDisable(state);
        visitDurationInput.setDisable(state);
        goToRandomPageCb.setDisable(state);
    }

    @FXML
    public void onStartClick() {
        if (visitsNumberInput.getText().isEmpty() || visitDurationInput.getText().isEmpty()) {
            Toast.makeText((Stage)ap.getScene().getWindow(), "Fill in all the fields!", 1000, 500, 500);
            return;
        }
        tabPane.getTabs().clear();
        changeFieldsState(true);
        int seconds = LocalTime.now().getHour()*3600 +
                LocalTime.now().getMinute()*60 +
                LocalTime.now().getSecond() +
                (Integer.parseInt(visitsNumberInput.getText())-1)*Integer.parseInt(visitDurationInput.getText());
        finishTimer.setText(""+LocalTime.of(seconds % (3600*24) / 3600, (seconds % 3600)/60%60, seconds % 60));
        visitorService.restart();
    }

    @FXML
    public void onStopClick(){
        changeFieldsState(false);
        visitorService.cancel();
        finishTimer.setText(""+LocalTime.of(0, 0)+":00");
        progressLabel.setText("0/0");
    }

    @FXML
    public void showBrowserHandler(){
        tabPane.setVisible(showBrowserCb.isSelected());
    }
}
