/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class EventSchedule extends DisplayEntity implements SampleProvider{

	@Keyword(description = "A sequence of monotonically-increasing simulation times at which to "
	                     + "generate events. If entered in date format, an input of "
	                     + "'0000-01-01 00:00:00' corresponds to zero simulation time.",
	         exampleList = {"2  10  18  h",
	                        "'0000-01-15 12:30:00'  '0000-02-07 8:00:00'  '0000-03-30 18:00:00'"})
	private final ValueListInput timeList;

	@Keyword(description = "Defines when the event times will repeat from the start.",
	         exampleList = {"8760.0 h"})
	private final ValueInput cycleTime;

	private int index = -1;
	private boolean firstSample = true;

	{
		timeList = new ValueListInput("TimeList", "Key Inputs", null);
		timeList.setUnitType(TimeUnit.class);
		timeList.setValidRange(0.0, Double.POSITIVE_INFINITY);
		timeList.setMonotonic(1);
		timeList.setRequired(true);
		this.addInput(timeList);

		cycleTime = new ValueInput("CycleTime", "Key Inputs", null);
		cycleTime.setUnitType(TimeUnit.class);
		cycleTime.setValidRange(0.0, Double.POSITIVE_INFINITY);
		cycleTime.setRequired(true);
		this.addInput(cycleTime);
	}


	public EventSchedule() {}

	@Override
	public void validate() {
		super.validate();
		DoubleVector list = timeList.getValue();
		if (list.get(list.size()-1) > cycleTime.getValue())
			throw new InputErrorException("The input for CycleTime must be greater than or equal "
					+ "to the last entry for TimeList.");
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		index = -1;
		firstSample = true;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return TimeUnit.class;
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Override
	public double getMinValue() {
		return 0;
	}

	@Override
	public double getMaxValue() {
		return 0;
	}

	@Output(name = "Index",
	 description = "The position of the event time in the list for the last inter-arrival time "
	             + "that was returned.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getIndexOfSample(double simTime) {
		return index+1;
	}

	@Output(name = "Value",
	 description = "The last inter-arrival time returned from the sequence. When used in an "
	             + "expression, this output returns a new value every time the expression "
	             + "is evaluated.",
	    unitType = TimeUnit.class,
	    sequence = 1)
	@Override
	public double getNextSample(double simTime) {
		DoubleVector list = timeList.getValue();

		if (list == null)
			return Double.NaN;

		// If called from a model thread, increment the index to be selected
		if (EventManager.hasCurrent()) {
			index = (index + 1) % list.size();
			if (firstSample && index > 0)
				firstSample = false;
		}

		// Trap an index that is out of range. Note that index can exceed the size of the list
		// if the TimeList keyword is edited in the middle of a run
		if (index < 0 || index >= list.size())
			return Double.NaN;


		if (index == 0) {
			// The first IAT calculated from the list is referenced to zero simulation time
			if (firstSample)
				return list.get(0);

			// All but the first IATs are referenced to the last time in the list
			return list.get(0) + cycleTime.getValue() - list.get(list.size()-1);
		}

		return list.get(index) - list.get(index-1);
	}

}
