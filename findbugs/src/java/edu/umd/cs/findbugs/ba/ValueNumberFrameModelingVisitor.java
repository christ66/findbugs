/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.daveho.ba;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;

public class ValueNumberFrameModelingVisitor extends AbstractFrameModelingVisitor<ValueNumber> {
	private ValueNumberFactory factory;
	private ValueNumberCache cache;

	public ValueNumberFrameModelingVisitor(ValueNumberFrame frame, ConstantPoolGen cpg, ValueNumberFactory factory,
		ValueNumberCache cache) {
		super(frame, cpg);
		this.factory = factory;
		this.cache = cache;
	}

	public ValueNumber getDefaultValue() {
		return factory.createFreshValue();
	}

	public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
		Frame<ValueNumber> frame = getFrame();

		// Get the input operands to this instruction.
		ValueNumber[] inputValueList = new ValueNumber[numWordsConsumed];

		// Pop off the input operands.
		try {
			frame.getTopStackWords(inputValueList);
			while (numWordsConsumed-- > 0) {
				frame.popValue();
			}
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.toString());
		}

		// See if we have the output operands in the cache.
		// If not, push default values for the output.
		ValueNumberCache.Entry entry = new ValueNumberCache.Entry(ins, inputValueList);
		ValueNumber[] outputValueList = cache.lookupOutputValues(entry);
		if (outputValueList == null) {
			outputValueList = new ValueNumber[numWordsProduced];
			for (int i = 0; i < numWordsProduced; ++i)
				outputValueList[i] = factory.createFreshValue();
			cache.addOutputValues(entry, outputValueList);
		}

		if (outputValueList.length != numWordsProduced)
			throw new IllegalStateException("cache produced wrong num words");

		// Push output operands on stack.
		for (int i = 0; i < outputValueList.length; ++i)
			frame.pushValue(outputValueList[i]);
	}

}

// vim:ts=4
