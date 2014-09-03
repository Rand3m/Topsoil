/*
 * Copyright 2014 zeringuej.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cirdles.topsoil;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.groupingBy;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import org.cirdles.topsoil.plugins.TopsoilPluginManager;
import org.cirdles.topsoil.table.Record;
import org.cirdles.topsoil.utils.TSVTableReader;
import org.cirdles.topsoil.utils.TSVTableWriter;
import org.cirdles.topsoil.utils.TableReader;
import org.cirdles.topsoil.utils.TableWriter;
import org.controlsfx.dialog.Dialogs;

/**
 * FXML Controller class
 *
 * @author zeringuej
 */
public class TopsoilController implements Initializable {

    @FXML private Node root;
    @FXML private TSVTable dataTable;
    @FXML private MenuBar menuBar;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataTable.setSavePath(Topsoil.LAST_TABLE_PATH);
        dataTable.load();
        
        buildChartsMenu();
    }

    public void buildChartsMenu() {
        Menu chartsMenu = new Menu("Charts");
        Path pluginsPath = Topsoil.TOPSOIL_PATH.resolve("Plugins");

        if (Files.exists(pluginsPath)) {
            TopsoilPluginManager pluginManager = new TopsoilPluginManager(pluginsPath);

            pluginManager.loadPlugins().stream()
                    .flatMap(plugin -> plugin.getCharts().stream())
                    .collect(groupingBy(chart -> chart.getCategory().orElse("Other")))
                    .forEach((category, charts) -> {
                        Menu submenu = new Menu(category);
                        
                        charts.stream()
                                .sorted(by(chart -> chart.getName().get()))
                                .forEach(chart -> submenu.getItems().add(new MenuItem(chart.getName().get())));
                        
                        chartsMenu.getItems().add(submenu);
                    });
            
            chartsMenu.getItems().sort(by(MenuItem::getText));
            menuBar.getMenus().add(chartsMenu);
        }
    }
    
    public static <T, K extends Comparable<K>> Comparator<T> by(Function<T, K> property) {
        return (thisT, thatT) -> property.apply(thisT).compareTo(property.apply(thatT));
    }

    @FXML
    private void importFromFile(ActionEvent event) {
        FileChooser tsvChooser = new FileChooser();
        tsvChooser.setInitialDirectory(Topsoil.USER_HOME.toFile());
        tsvChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Table Files", "TSV"));
        Path filePath = tsvChooser.showOpenDialog(root.getScene().getWindow()).toPath();

        Tools.yesNoPrompt("Does the selected file contain headers?", response -> {
            TableReader tableReader = new TSVTableReader(response);

            try {
                tableReader.read(filePath, dataTable);
            } catch (IOException ex) {
                Logger.getLogger(Topsoil.class.getName()).log(Level.SEVERE, null, ex);
            }

            TableWriter<Record> tableWriter = new TSVTableWriter(true);
            tableWriter.write(dataTable, Topsoil.LAST_TABLE_PATH);
        });
    }

    @FXML
    private void createErrorChart(ActionEvent event) {
        // table needs 5 columns to generate chart
        if (dataTable.getColumns().size() < 5) {
            Dialogs.create().message(Topsoil.NOT_ENOUGH_COLUMNS_MESSAGE).showWarning();
        } else {
            new ColumnSelectorDialog(dataTable).show();
        }
    }

    @FXML
    private void pasteFromClipboard(ActionEvent event) {
        dataTable.pasteFromClipboard();
    }

    @FXML
    private void emptyTable(ActionEvent event) {
        dataTable.clear();
        dataTable.save();
    }
}
