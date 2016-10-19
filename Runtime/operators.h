#pragma once

// Unwraps Lvalues to their raw value for operations
void _unwrap() {
	CalculatorValue* value = stack->pop();
	if (!value->isLvalue()) {
		stack->push(value);
	} else {
		CalculatorValue unwrappedValue = CalculatorValue(*value->lvalue());
		stack->push(&unwrappedValue);
	}
	delete value;
}

void rightArrowOperator() {
	_unwrap();
	CalculatorValue* rhs = stack->pop();
	_unwrap();
	CalculatorValue* lhs = stack->pop();
	if (!rhs->isStandardOut()) { printf("RHS value is not stdout\n"); exit(1); }
	switch (lhs->getType()->getType()) {
		case NullType:
		case IdentityType:
			printf("Cannot print\n");
			exit(1);
			break;
//		case BooleanType:
		case IntegerType:
//		case RealType:
//		case CharacterType:
//		case IntervalType:
//		case VectorType:
			stack->push(lhs);
			printValue();
			break;
//		case MatrixType:
//		case TupleType:
		case StandardOut:
		case StandardIn:
//		case VectorStart:
		case Lvalue:
			printf("Cannot print\n");
			exit(1);
			break;
	}
	delete lhs;
	delete rhs;
}

void leftArrowOperator() {
	_unwrap();
	CalculatorValue* rhs = stack->pop();
	CalculatorValue* lhs = stack->pop();
	if (!rhs->isStandardIn()) { printf("RHS value is not stdin\n"); exit(1); }
	if (!lhs->isLvalue()) { printf("LHS not an Lvalue"); exit(1); }
	int intValue;
	switch (lhs->lvalue()->getType()->getType()) {
		case NullType:
		case IdentityType:
			printf("Cannot determine type\n");
			exit(1);
			break;
		case IntegerType:
			scanf("%d", &intValue);
			*lhs->lvalue_raw() = new CalculatorValue(intValue);
			break;
		case StandardOut:
		case StandardIn:
		case Lvalue:
			printf("Cannot input\n");
			exit(1);
			break;
	}
	delete lhs;
	delete rhs;
}
