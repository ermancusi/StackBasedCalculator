/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UserDefinedOperation.StackOperations;

import Stack.ObservableStack;
import Operations.OperationsEnum;
import Operations.SupportedOperation;
import org.apache.commons.math3.complex.Complex;

/**
 *
 * @author Speranza
 */
public class SwapOperation extends SupportedOperation {

    public SwapOperation(ObservableStack<Complex> stack) {
        super(OperationsEnum.SWAP,stack);
    }

    /**
     *  Swaps the last two elements into the stack.
     * 
     */
    @Override
    public void execute() {
        super.swap();
    }

}