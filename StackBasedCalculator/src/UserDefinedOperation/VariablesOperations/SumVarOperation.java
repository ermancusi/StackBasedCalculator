package UserDefinedOperation.VariablesOperations;

import Stack.ObservableStack;
import Operations.OperationsEnum;
import Operations.SupportedOperation;
import VariablesManager.VariablesStorage;
import org.apache.commons.math3.complex.Complex;

/**
 *
 * @author Speranza
 */
public class SumVarOperation extends SupportedOperation {

    private final VariablesStorage variableManager;
    private final String variableName;

    public SumVarOperation(ObservableStack<Complex> stack,VariablesStorage variableManager, String variableName) {
        super(OperationsEnum.SUM_VAR, stack);
        this.variableManager = variableManager;
        this.variableName = variableName;
    }

    /**
     * Sums the top of the stack to a stored variable value.
     *
     */
    @Override
    public void execute() {
        super.push(this.variableManager.getVariableValue(this.variableName));
    }

}
