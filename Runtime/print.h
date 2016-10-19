#pragma once

bool shouldPrintNewLine = true;

//void printBoolean(CalculatorValue* value) {
//	if (*value->booleanValue()) {
//		printf("T");
//	} else {
//		printf("F");
//	}
//}

void printInteger(CalculatorValue* value) {
	printf("%d", *value->integerValue());
}

//void printReal(CalculatorValue* value) {
//	printf("%g", *value->realValue());
//}
//
//void printCharacter(CalculatorValue* value) {
//	printf("%c", *value->characterValue());
//}
//
//void printVector(CalculatorValue* value) {
//	bool oldShouldPrintNewLine = shouldPrintNewLine;
//	shouldPrintNewLine = false;
//	Vector<CalculatorValue>* vector = value->vectorValue();
//	for (int i = 0; i < vector->getCount(); ++i) {
//		stack->push(vector->get(i));
//		printValue();
//		if (i + 1 < vector->getCount()) {
//			printf(", ");
//		}
//	}
//	shouldPrintNewLine = oldShouldPrintNewLine;
//}
//
//void printString(CalculatorValue* value) {
//	for (int i = 0; i < value->vectorValue()->getCount(); ++i) {
//		stack->push(value->vectorValue()->get(i));
//		printValue();
//	}
//}
//
//void printTuple(CalculatorValue* value) {
//	printf("(");
//	printVector(value);
//	printf(")");
//}

void printValue() {
	CalculatorValue* value = stack->pop();
	switch (value->getType()->getType()) {
		case NullType:
		case IdentityType:
			printf("INVALID PRINT CALL\n");
			exit(1);
//		case BooleanType:
//			printBoolean(value);
//			break;
		case IntegerType:
			printInteger(value);
			break;
//		case RealType:
//			printReal(value);
//			break;
//		case CharacterType:
//			printCharacter(value);
//			break;
//		case IntervalType:
//		case VectorType:
//			if (value->getType()->getSubtypes()->getCount() > 0 &&
//				value->getType()->getSubtypes()->get(0)->getType() ==
//				                                                CharacterType) {
//				printString(value);
//			} else {
//				printVector(value);
//			}
//			break;
//		case MatrixType:
//			printf("Cannot print Matrix\n");
//			exit(1);
//		case TupleType:
//			printf("Cannot print Tuple\n");
//			exit(1);
		case StandardOut:
		case StandardIn:
//		case VectorStart:
		case Lvalue:
			printf("INVALID PRINT CALL\n");
			exit(1);
	}
	delete value;
	if (shouldPrintNewLine) {
		printf("\n");
	}
}
