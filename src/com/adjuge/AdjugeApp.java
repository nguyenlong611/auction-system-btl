package com.adjuge;

import com.adjuge.dao.DatabaseManager;
import com.adjuge.pattern.DataStore;
import com.adjuge.service.AuctionTimerService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * BidVault Application Launcher.
 *
 * This is the entry point of the JavaFX application.
 * It initializes seed data, loads the FXML UI, and starts the auction timer.
 */
public class AdjugeApp extends Application {

    private AuctionTimerService timerService;

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Load bundled fonts from classpath (works regardless of what's installed on the machine)
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-SemiBold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlayfairDisplay-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlayfairDisplay-Bold.ttf"), 14);

        // 2. Initialize seed data (Singleton pattern — DataStore)
        DataStore.getInstance().seedData();

        // 2. Load the FXML layout
        Parent root = FXMLLoader.load(getClass().getResource("/bidding.fxml"));

        // 3. Create the scene
        Scene scene = new Scene(root);
        stage.setTitle("Adjuge");
        stage.setScene(scene);
        stage.setWidth(1440);
        stage.setHeight(900);
        stage.show();

        // 4. Start the auction timer (auto-closes expired auctions)
        timerService = new AuctionTimerService();
        timerService.start();
    }

    @Override
    public void stop() {
        // Clean up the timer when the app closes
        if (timerService != null) {
            timerService.stop();
        }
        // Close the database connection
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
