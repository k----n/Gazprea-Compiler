#pragma once

void std_output() {
	Type* type = new Type(StandardOut);
	CalculatorValue* value = new CalculatorValue(type, nullptr);
	stack->push(value);
	delete value;
}

void std_input() {
	CalculatorValue* value = new CalculatorValue(new Type(StandardIn), nullptr);
	stack->push(value);
	delete value;
}
