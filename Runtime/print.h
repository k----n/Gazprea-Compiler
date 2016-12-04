#pragma once

void printBoolean(Value* value) {
	bool* boolValue = value->booleanValue();
	if (*boolValue) {
		printf("T");
	} else {
		printf("F");
	}
	delete boolValue;
}

void printInteger(Value* value) {
	int* intValue_ptr = value->integerValue();
	printf("%d", *intValue_ptr);
	delete intValue_ptr;
}

void printReal(Value* value) {
	float* realValue_ptr = value->realValue();
	printf("%g", *realValue_ptr);
	delete realValue_ptr;
}

void printCharacter(Value* value) {
	printf("%c", *value->characterValue());
}

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
	Value* value = stack->pop();
	ValueType* valueType = value->getType();

    // following two declarations are meant for printing a vector
    int vectorSize;
	int matrixSize;
    Value *vectorElement;

	Value *newChar; // only for nulltype and identitytype
	switch (valueType->getType()) {
		case NullType:
		    newChar = new Value('\0');
		    printCharacter(newChar);
		    break;
		case IdentityType:
		    newChar = new Value('\1');
		    printCharacter(newChar);
		    break;
		case BooleanType:	printBoolean(value);					break;
		case IntegerType:	printInteger(value);					break;
		case RealType:		printReal(value);						break;
		case CharacterType:	printCharacter(value);					break;
		case TupleType:     printf("Cannot print TupleType\n");     exit(1);
		case IntervalType:  printf("Cannot print IntervalType\n");  exit(1);
		case VectorType:
		    vectorSize = value->vectorValue()->getCount();
		    for (int elem = 0; elem < vectorSize; ++elem) {
                vectorElement = value->vectorValue()->get(elem);
                switch (vectorElement->getType()->getType()) {
                    case IntegerType:
                        printInteger(vectorElement);
                        break;
                    case RealType:
                        printReal(vectorElement);
                        break;
                    case CharacterType:
                        printCharacter(vectorElement);
                        break;
                    case BooleanType:
                        printBoolean(vectorElement);
                        break;
                    default:
                        printf("Vector element of this type cannot be printed\n"); exit(1);
                }
		    }
		    break;
		case MatrixType:
			matrixSize = value->matrixValue()->getCount();
			for (int i = 0; i < matrixSize; ++i) {
				vectorElement = value->matrixValue()->get(i);
				stack->push(vectorElement);
				printValue();
			}
			break;
		case StandardOut:	printf("Cannot print StandardOut\n");	exit(1);
		case StandardIn:	printf("Cannot print StandardIn\n");	exit(1);
		case Lvalue:		printf("Cannot print Lvalue\n");		exit(1);
		case StartVector:	printf("Cannot print StartVector\n");	exit(1);
	}
	valueType->release();
	value->release();
}
