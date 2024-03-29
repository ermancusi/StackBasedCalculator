package UserInterface;

import Operations.Operation;
import Operations.UserDefinedOperation;
import ArchiveModule.Archive;
import Operations.OperationsEnum;
import Stack.ObservableStack;
import Operations.StackOperations.*;
import Operations.VariablesOperation;
import UserInterface.CellFactory.*;
import UserInterface.Parser.ParserEnum;
import VariablesManager.VariablesStorage;
import java.util.*;
import java.util.stream.Collectors;
import javafx.beans.binding.*;
import javafx.collections.*;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexFormat;
import UserInterface.Parser.ParserFactory;
import javafx.util.Callback;
import java.io.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser;

/**
 * Implementation of the Calculator User Interface Controller
 *
 * @author Speranza
 * @author ermancusi
 */
public class CalculatorController {

    //FXML components
    @FXML
    private Pane titlePane, calculatorPane, operationsPane;
    @FXML
    private ImageView btnMinimize, btnClose;
    @FXML
    private TextArea textAreaCalculator;
    @FXML
    private TextField inputNumber, inputName;
    @FXML
    private Button btnClearEntry, btnPush, btnFinalCreate, btnDelete;
    @FXML
    private Text textWarning, textWarningSoft;
    @FXML
    private TableView<String> tableVariables;
    @FXML
    private TableColumn<String, String> columnVariable;
    @FXML
    private TableColumn<String, Complex> columnValue;
    @FXML
    private ListView<Operation> operationsList;
    @FXML
    private ListView<UserDefinedOperation> userDefinedList, definedOperationsList;
    @FXML
    private ListView<Operation> finalList;
    @FXML
    private ListView<Complex> stackList;

    //definition of utils variables
    private double xAxis, yAxis;
    //Variable Storage for calculator
    private VariablesStorage variableStorage;
    // Archive for save & restore operations
    private Archive variablesArchive;
    // List used in User-Defined operations definitions in UI
    private ObservableList<UserDefinedOperation> UserDefinedOperations;
    private ObservableList<Operation> finalObservable;
    private ObservableList<Operation> operationsObservable;
    //Attributes for create a new operation
    private SimpleFactoryCommand commandCreator;
    //Stack with operand 
    private ObservableStack<Complex> stack;
    //Parser for user's input
    private ParserFactory parser;

    /**
     * Initializes the User Interface. It's executed as soon as the program
     * starts.
     *
     * @return
     */
    public void init(Stage stage) {
        Scene scene = stage.getScene();
        // Stack used for visualization
        stack = new ObservableStack<>();
        parser = new ParserFactory();
        //create observable lists and set them to the respective lists (components).
        finalObservable = FXCollections.observableArrayList();
        finalList.setItems(finalObservable);

        operationsObservable = FXCollections.observableArrayList();
        operationsList.setItems(operationsObservable);
        operationsList.setCellFactory(new OperationCellFactory());

        UserDefinedOperations = FXCollections.observableArrayList();
        userDefinedList.setItems(UserDefinedOperations);
        definedOperationsList.setItems(UserDefinedOperations);

        // Set list cell for complex number visualization
        stackList.setCellFactory(new NumberListFactory());
        stack.setObserver(stackList);

        //set table cell columns for variables visualization
        variableStorage = new VariablesStorage();
        columnValue.setCellFactory(new NumberColumnFactory());
        variableStorage.setObserver(tableVariables, columnVariable, columnValue);

        //set archive for VariableStorage
        variablesArchive = new Archive(variableStorage);
        //Create the command factory
        commandCreator = new SimpleFactoryCommand(this.stack, this.variableStorage);
        commandCreator.setArchive(variablesArchive);
        //populate tables
        populate();

        // Set bindings for warning
        BooleanBinding oneElements = Bindings.size(stackList.getItems()).
                isEqualTo(1).and(textAreaCalculator.textProperty().isEqualTo("swap").
                or(textAreaCalculator.textProperty().isEqualTo("over")));

        BooleanBinding twoElements = Bindings.size(stackList.getItems()).
                lessThan(2).and(textAreaCalculator.textProperty().isEqualTo("+").
                or(textAreaCalculator.textProperty().isEqualTo("-").
                        or(textAreaCalculator.textProperty().isEqualTo("/").
                                or(textAreaCalculator.textProperty().isEqualTo("*")))));
        BooleanBinding emptyList = Bindings.size(stackList.getItems()).
                isEqualTo(0).and(textAreaCalculator.textProperty().isEqualTo("+-").
                or(textAreaCalculator.textProperty().isEqualTo("sqrt")));

        textWarning.visibleProperty().bind(oneElements.or(twoElements).or(emptyList));
        textWarningSoft.visibleProperty().bind(Bindings.size(stackList.getItems()).
                greaterThan(0).and(textAreaCalculator.textProperty().isEqualTo("clear")));

        //btnPush is disabled when the Text Area is empty
        btnPush.disableProperty().bind(Bindings.createBooleanBinding(()
                -> textAreaCalculator.getText().trim().isEmpty(),
                textAreaCalculator.textProperty()));

        //btnFinalCreate is disable if the list finalObservable is empty or inputName is empty or inputNumber is empty
        btnFinalCreate.disableProperty().bind((Bindings.size(finalObservable).isEqualTo(0)).or(inputName.textProperty().isEmpty()).or(inputNumber.textProperty().isEmpty()));

        //btnDelete is disable if finalObservable does not contain elements.
        btnDelete.disableProperty().bind((Bindings.size(finalObservable).isEqualTo(0)));

        //when the user presses the "back space" button on physical keyboard
        //the last element in the specific text area is deleted.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && textAreaCalculator.getText().length() > 0) {
                push(new ActionEvent());

            }
            if (e.getCode() == KeyCode.BACK_SPACE && textAreaCalculator.getText().length() > 0) {
                textAreaCalculator.setText(textAreaCalculator.getText().substring(0, textAreaCalculator.getText().length() - 1));
                textAreaCalculator.end();
            }
            if (e.getCode() == KeyCode.BACK_SPACE && inputName.getText().length() > 0 && inputName.isFocused()) {
                inputName.setText(inputName.getText().substring(0, inputName.getText().length() - 1));
                inputName.end();
            }
            if (e.getCode() == KeyCode.BACK_SPACE && inputNumber.getText().length() > 0 && inputNumber.isFocused()) {
                inputNumber.setText(inputNumber.getText().substring(0, inputNumber.getText().length() - 1));
                inputNumber.end();
            }

            e.consume();
        });

        //if the user presses the "back space" button on physical keyboard
        //for more than 0.2 seconds the entire Text Area is cleaned up.
        btnClearEntry.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            long startTime;

            @Override
            public void handle(MouseEvent event) {
                if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
                    startTime = System.currentTimeMillis();
                } else if (event.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
                    if (System.currentTimeMillis() - startTime > 0.2 * 1000) {
                        textAreaCalculator.setText("");
                    }
                }
            }
        });

        //menu items to allow the execution and the delete of a user defined operation
        MenuItem deleteMenu = new MenuItem("Delete");
        MenuItem executeMenu = new MenuItem("Execute");
        MenuItem exportMenu = new MenuItem("Export...");

        ContextMenu contextMenu = new ContextMenu(executeMenu, deleteMenu, exportMenu);
        definedOperationsList.setCellFactory(ContextMenuListCell.<UserDefinedOperation>forListView(contextMenu));

        //actions executed when the user selects the "Delete" option from the menu
        deleteMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                //alert for confirm the deletion
                Optional<ButtonType> result = createAlert(AlertType.CONFIRMATION,
                        "Confirmation Dialog", "Look, a Confirmation Dialog",
                        "Do you confirm that you want to cancel this operation?");
                //if the user confirms the deletion
                if (result.get() == ButtonType.OK) {
                    //pick the user defined operation to delete
                    UserDefinedOperation userDefineToDelete = definedOperationsList.getSelectionModel().getSelectedItem();
                    if (userDefineToDelete == null) {
                        return;
                    }
                    //if it is never used in others user defined operations, then delete it
                    if (deleteUserDefinedOperation(userDefineToDelete)) {
                        UserDefinedOperations.remove(userDefineToDelete);
                        createAlert(AlertType.INFORMATION, "Information Dialog",
                                "Information Message",
                                "Operation deleted correctly.");

                    } else {//otherwise don't delete it
                        createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                                "\nImpossible to delete.\nThis operation is contained inside others operations");
                    }

                } else { //if the user doesn't confirms the deletion simply do nothing
                    return;
                }
            }
        });

        //actions executed when the user selects the "Execute" option from the menu
        executeMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                //user defined operation selected by the user
                UserDefinedOperation userDefineToExecute = definedOperationsList.getSelectionModel().getSelectedItem();
                if (userDefineToExecute == null) {
                    return;
                }
                // number of operands expected
                int numOperands = userDefineToExecute.getRequiredOperands();
                //if the stack contains less then the operands required
                //an error message appears and the execution fails
                if (numOperands > stack.size()) {
                    createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                            "\nImpossible to execute.\nInsufficient number of operands.");
                } else {
                    //the stack contains the number of operands required
                    //but it can be performed the division by 0
                    try {
                        userDefineToExecute.execute();
                    } catch (ArithmeticException ex) {
                        createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                                "\nError during execution - MATH ERROR\n");
                    }
                }

            }
        });

        exportMenu.setOnAction((ActionEvent e) -> {
            UserDefinedOperation toExport = definedOperationsList.getSelectionModel().getSelectedItem();
            if (toExport != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialFileName(toExport.getName());
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Text Files", "*.txt")
                );
                File selectedFile = fileChooser.showSaveDialog(stage);
                if (selectedFile != null) {
                    try {
                        toExport.exportOperation(selectedFile.getAbsoluteFile().getAbsolutePath());
                    } catch (IOException ex) {
                        createAlert(AlertType.ERROR, "Look, an error!", "Output error", ex.getMessage());
                    }
                }
                definedOperationsList.getSelectionModel().clearSelection();
            }
        });

        //MenuContext to execute variables operations on thier TableView
        tableVariables.setRowFactory(new Callback<TableView<String>, TableRow<String>>() {
            @Override
            public TableRow<String> call(TableView<String> param) {
                final TableRow<String> row = new TableRow<>();

                MenuItem loadVar = new MenuItem("<");
                loadVar.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        commandCreator.setOperation(OperationsEnum.LOAD);
                        commandCreator.setVariableName(param.getSelectionModel().getSelectedItem());
                        executeCommandSafely(commandCreator.pickCommand());
                    }
                });

                MenuItem saveVar = new MenuItem(">");
                saveVar.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        commandCreator.setOperation(OperationsEnum.SAVE);
                        commandCreator.setVariableName(param.getSelectionModel().getSelectedItem());
                        executeCommandSafely(commandCreator.pickCommand());
                    }
                });

                MenuItem sumToVar = new MenuItem("+");
                sumToVar.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        commandCreator.setOperation(OperationsEnum.SUM_VAR);
                        commandCreator.setVariableName(param.getSelectionModel().getSelectedItem());
                        executeCommandSafely(commandCreator.pickCommand());
                    }
                });

                MenuItem subToVar = new MenuItem("-");
                subToVar.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        commandCreator.setOperation(OperationsEnum.SUB_VAR);
                        commandCreator.setVariableName(param.getSelectionModel().getSelectedItem());
                        executeCommandSafely(commandCreator.pickCommand());
                    }
                });

                final ContextMenu contextMenuVar = new ContextMenu(loadVar, saveVar, sumToVar, subToVar);

                row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(contextMenuVar));
                return row;
            }

            private void executeCommandSafely(Operation op) {
                try {
                    op.execute();
                } catch (NoSuchElementException | ArithmeticException e) {
                    createAlert(AlertType.ERROR, "Error", "Look, an Error!", "\nImpossible to continue.\n" + e.getMessage());
                }
            }
        });

        //close the Calculator
        btnClose.setOnMouseClicked(mouseEvent -> stage.close());
        //minimize the Calculator
        btnMinimize.setOnMouseClicked(mouseEvent -> stage.setIconified(true));

        //allows the user to move around, on the screen, the calculator
        titlePane.setOnMousePressed(mouseEvent -> {
            xAxis = mouseEvent.getSceneX();
            yAxis = mouseEvent.getSceneY();
        });
        titlePane.setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() - xAxis);
            stage.setY(mouseEvent.getScreenY() - yAxis);
        });
    }

    /**
     * Gets the number associated with the on-screen keyboard button and shows
     * it in the User Interface Text Area.
     *
     * @return
     */
    @FXML
    private void onNumberPress(ActionEvent event) {
        String number = ((Button) event.getSource()).getText();
        textAreaCalculator.setText(textAreaCalculator.getText() + number);
    }

    /**
     * Gets the operation associated with the on-screen keyboard button and
     * shows it in the User Interface Text Area.
     *
     * @return
     */
    @FXML
    private void onOperationPress(ActionEvent event) {
        String operation = ((Button) event.getSource()).getText();
        textAreaCalculator.setText(textAreaCalculator.getText() + operation);
    }

    /**
     * When the "Clear entry" (⌫) button is pressed, the last item of the Text
     * Area is cleaned up.
     *
     * @return
     */
    @FXML
    private void deleteLast(ActionEvent event) {
        if (textAreaCalculator.getText().length() > 0) {
            textAreaCalculator.setText(textAreaCalculator.getText()
                    .substring(0, textAreaCalculator.getText().length() - 1));
        }
    }

    /**
     * When the "push" (↑) button is pressed, the value in the Text Area is
     * pushed in the stack if it's a number, otherwise is executed the operation
     * on the variable indicated by the user and entered it in the associated
     * table. The function checks if the input is in a right format and checks
     * if the user enters the operations supported by the Calculator.
     *
     * @return
     */
    @FXML
    private void push(ActionEvent event) {

        //define of used variables
        String input = textAreaCalculator.getText();
        textAreaCalculator.clear();
        OperationsEnum operation;

        String supportedVariable = parser.getParser(ParserEnum.VARIABLE).check(input);
        Operation toExecute = null;
        Complex number;

        try {
            operation = OperationsEnum.valueOfString(parser.getParser(ParserEnum.OPERATION).check(input));
        } catch (UnsupportedOperationException e) {
            operation = null;
        }

        try {
            number = new ComplexFormat().parse(parser.getParser(ParserEnum.COMPLEXNUMBER).check(input));
        } catch (NullPointerException e) {
            number = null;
        }

        try {
            //Input is recognized as Complex number, then perform a push onto the stack
            if (number != null) {
                this.commandCreator.setOperation(OperationsEnum.PUSH);
                this.commandCreator.setNumber(number);
                toExecute = this.commandCreator.pickCommand();
            } else if (operation != null) {
                //Input is recognized as an Arithmetical or Stack operation, then select it
                this.commandCreator.setOperation(operation);
                toExecute = this.commandCreator.pickCommand();
            } else if (supportedVariable != null) {
                // Input is recognized as variable operation, then set the variable and execute it
                String varOperation = supportedVariable.substring(0, 1);
                String variable = supportedVariable.substring(1);
                this.commandCreator.setOperation(OperationsEnum.valueOfString(varOperation + "var"));
                this.commandCreator.setVariableName(variable);
                toExecute = this.commandCreator.pickCommand();
            }
            //Execute the operation
            toExecute.execute();
            //Scroll stack view
            stackList.scrollTo(stackList.getItems().size());
        } catch (NoSuchElementException | ArithmeticException e) {
            createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                    "\nImpossible to continue.\n" + e.getMessage());
        } catch (NullPointerException ex) {
            createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                    "\nInvalid input inserted:\n" + input);
        }
    }

    /**
     * Allows the User to view the window to define a custom operation.
     *
     * @return
     */
    @FXML
    private void onCreatePress(ActionEvent event) {
        operationsPane.setVisible(true);
        operationsPane.setDisable(false);
        calculatorPane.setVisible(false);
        calculatorPane.setDisable(true);
    }

    /**
     * Allows the User to create a custom operation after its definition.
     *
     * @return
     */
    @FXML
    private void onFinalCreatePress(ActionEvent event) {
        String name = inputName.getText();
        String operandsNumber = inputNumber.getText();

        if (name.contains("$") || name.contains("£") || name.contains("#")
                || name.contains("!") || name.contains("?") || name.contains("%") || name.contains("&")) {
            createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                    "The operation name can’t contain special characters ($, £,#,!,?,%,&)");
            return;
        }

        try {
            Integer.parseInt(operandsNumber);
        } catch (Exception e) {
            createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                    "The operands number must be an integer!");
            return;
        }

        int operatorsNumber = Integer.parseInt(inputNumber.getText());
        UserDefinedOperation u = new UserDefinedOperation(name, operatorsNumber, finalObservable.stream().collect(Collectors.toList()));

        if (UserDefinedOperations.contains(u)) {
            createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                    "An operation with this name already exists, please change it.");
            return;
        }
        UserDefinedOperations.add(u);
        finalObservable.clear();
        inputName.clear();
        inputNumber.clear();
        createAlert(AlertType.INFORMATION, "Operation created", "The operation is created properly!", "");

    }

    /**
     * Allows the User to delete the last inserted operation during the
     * definition of a custom operation
     *
     * @return
     */
    @FXML
    private void onDeletePress(ActionEvent event) {
        if (finalObservable.size() > 0) {
            finalObservable.remove(finalObservable.size() - 1);
        }
    }

    /**
     * Allows the User to go back to the calculator's view (the first view)
     *
     * @return
     */
    @FXML
    private void onBackPress(ActionEvent event) {
        calculatorPane.setVisible(true);
        calculatorPane.setDisable(false);
        operationsPane.setVisible(false);
        operationsPane.setDisable(true);
    }

    /**
     * Allows the User to insert a supported operation in the list that gather
     * all the operations inserted by the user in the phase of definition of a
     * custom operation (the list in the bottom-right corner)
     *
     * @return
     */
    @FXML
    private void onInsertSupportedPress(ActionEvent event) {

        if (operationsList.getSelectionModel().isSelected(operationsList.getSelectionModel().getSelectedIndex())) {
            Operation op = operationsList.getSelectionModel().getSelectedItem();

            //selected th push operation
            if (op.toString().equalsIgnoreCase("push")) {
                Optional<String> result = createTextInputDialog("Push Operation",
                        "Please, insert a complex number", "insert here:");
                if (result.isPresent()) {
                    Complex num;
                    try {
                        num = new ComplexFormat().parse(parser.getParser(ParserEnum.COMPLEXNUMBER).check(result.get()));
                    } catch (NullPointerException e) {
                        num = null;
                    }
                    if (num == null) {
                        createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                                "Invalid complex number inserted:\n" + result.get());
                        return;
                    }
                    finalObservable.add(new PushOperation(stack, num));
                }

            } else if (op instanceof VariablesOperation) {
                VariablesOperation selected = (VariablesOperation) op;
                Optional<String> result = createTextInputDialog("Variable Operation",
                        "Please, insert a variable name (a-z)", "insert here:");
                if (result.isPresent()) {
                    String variableName = parser.getParser(ParserEnum.VARIABLE).check(op.toString().substring(0, 1) + result.get());

                    if (variableName == null) {
                        createAlert(AlertType.ERROR, "Error", "Look, an Error!",
                                "Invalid variable name:\n" + result.get());
                        return;
                    }
                    this.commandCreator.setOperation(OperationsEnum.valueOfString(selected.getName()));
                    this.commandCreator.setVariableName(variableName.substring(1, 2));
                    finalObservable.add(this.commandCreator.pickCommand());

                }

            } else {
                finalObservable.add(operationsList.getSelectionModel().getSelectedItem());
            }
            //finalList autoscroll enable
            finalList.scrollTo(finalList.getItems().size());

            operationsList.getSelectionModel().clearSelection();
        }

    }

    /**
     * Allows the User to insert a user defined operation in the list that
     * gather all the operations inserted by the user in the phase of definition
     * of a custom operation (the list in the bottom-right corner) (in this way
     * he creates a nested user defined operation)
     *
     * @return
     */
    @FXML
    private void onInsertDefinedPress(ActionEvent event) {
        if (userDefinedList.getSelectionModel().isSelected(userDefinedList.getSelectionModel().getSelectedIndex())) {
            finalObservable.add(userDefinedList.getSelectionModel().getSelectedItem());
            userDefinedList.getSelectionModel().clearSelection();
        }
    }

    /**
     * Allows the User to saveState the state of current variables.
     *
     * @return
     */
    @FXML
    private void onSavePress(ActionEvent event) throws CloneNotSupportedException, Exception {
        if (variableStorage.getSize() == 0) {
            return;
        }
        this.commandCreator.setOperation(OperationsEnum.SAVE_STATE);
        this.commandCreator.pickCommand().execute();
        createAlert(AlertType.INFORMATION, "Save Variable State",
                "Information Message", "Variables State saved properly");
    }

    /**
     * Allows the User to setCurrentState the state of variables.
     *
     * @return
     */
    @FXML
    private void onRestorePress(ActionEvent event) {
        try {
            this.commandCreator.setOperation(OperationsEnum.RESTORE_STATE);
            this.commandCreator.pickCommand().execute();
        } catch (EmptyStackException e) {
            createAlert(AlertType.ERROR, "Restore Variable State",
                    "Error Message", "There isn't a state to restore");
            return;
        }
        createAlert(AlertType.INFORMATION, "Restore Variable State",
                "Information Message", "Variables State restored properly");
    }

    //UTILS METHOD
    /**
     * Util method to create a text input dialog
     *
     * @return
     */
    private Optional<String> createTextInputDialog(String title, String header, String content) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        return dialog.showAndWait();
    }

    /**
     * Util method to create an alert
     *
     * @return
     */
    private Optional<ButtonType> createAlert(AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();

    }

    /**
     * Util method to populate the list with al the operations (non custom)
     * usable from the user.
     *
     * @return
     */
    private void populate() {
        for (OperationsEnum op : OperationsEnum.values()) {
            this.commandCreator.setOperation(op);
            Operation operation = this.commandCreator.pickCommand();
            operationsObservable.add(operation);
        }
    }

    /**
     * Util method to check if a user defined operation is contained inside at
     * least another user defined operation
     *
     * @return
     */
    private boolean deleteUserDefinedOperation(UserDefinedOperation toDelete) {
        for (UserDefinedOperation u : UserDefinedOperations) {
            if (u.contains(toDelete)) {
                return false;
            }
        }
        return true;
    }

    private UserDefinedOperation readOperationFromFile(String filename, List<UserDefinedOperation> userDefinedOperations) {
        UserDefinedOperation ret = null;
        try ( Scanner sc = new Scanner(new FileReader(filename))) {
            sc.useDelimiter("\n");
            String opName = sc.next();
            int operands = sc.nextInt();
            List<Operation> operationList = new ArrayList<>();
            while (sc.hasNext()) {
                String operationName = sc.next();
                Operation toInsert = null;
                // Try to match operation with basic operation
                String op = this.parser.getParser(ParserEnum.OPERATION).check(operationName);
                String supportedVariable = parser.getParser(ParserEnum.VARIABLE).check(operationName);
                String complexNumber = this.parser.getParser(ParserEnum.PUSH_FILE).check(operationName);
                if (op != null) {
                    OperationsEnum selectedOperation = OperationsEnum.valueOfString(op);
                    this.commandCreator.setOperation(selectedOperation);
                    toInsert = this.commandCreator.pickCommand();
                } // Try to match operation with variables operation
                else if (supportedVariable != null) {
                    String varOperation = supportedVariable.substring(0, 1);
                    String variable = supportedVariable.substring(1);
                    this.commandCreator.setOperation(OperationsEnum.valueOfString(varOperation + "var"));
                    this.commandCreator.setVariableName(variable);
                    toInsert = this.commandCreator.pickCommand();
                } else if (operationName.equals("save") || operationName.equals("restore")) {
                    //verify if it is save or restore operation
                    this.commandCreator.setOperation(OperationsEnum.valueOfString(operationName));
                    toInsert = this.commandCreator.pickCommand();
                } else if (complexNumber != null) {
                    // check if it is a push operation
                    Complex number = new ComplexFormat().parse(complexNumber);
                    this.commandCreator.setOperation(OperationsEnum.PUSH);
                    this.commandCreator.setNumber(number);
                    toInsert = this.commandCreator.pickCommand();
                } else {
                    for (UserDefinedOperation o : userDefinedOperations) {
                        if (o.getName().equals(operationName)) {
                            toInsert = o;
                        }
                    }
                }
                if (toInsert == null) {
                    return null;
                }
                operationList.add(toInsert);
            }
            ret = new UserDefinedOperation(opName, operands, operationList);
        } catch (Exception ex) {
            return null;
        }
        return ret;
    }

    @FXML
    private void onLoadFromFilePress(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        File selectedFile = fileChooser.showOpenDialog(calculatorPane.getScene().getWindow());
        if (selectedFile != null) {
            UserDefinedOperation toAdd = this.readOperationFromFile(selectedFile.getAbsolutePath(), UserDefinedOperations);
            if (toAdd != null) {
                if (UserDefinedOperations.contains(toAdd)) {
                    createAlert(AlertType.WARNING, "Error in reading file",
                            "Error Message", "There is already an operation with this name");
                } else {
                    UserDefinedOperations.add(toAdd);
                }
            } else {
                createAlert(AlertType.ERROR, "Error in reading file",
                        "Error Message", "Malformed file");
            }
        }
    }
}
