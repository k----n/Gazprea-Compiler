#pragma once

void pushNull() {
	CalculatorValue nullValue = CalculatorValue(new Type(NullType), nullptr);
	stack->push(&nullValue);
}

void pushIdentity() {
	CalculatorValue identityValue = CalculatorValue(new Type(IdentityType), nullptr);
	stack->push(&identityValue);
}

void pushInteger(int value) {
	CalculatorValue integerValue = CalculatorValue(value);
	stack->push(&integerValue);
}

void varInitPushNullInteger() {
	pushInteger(0);
}
