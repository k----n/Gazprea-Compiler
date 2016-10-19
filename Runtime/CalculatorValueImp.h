#pragma once

//CalculatorValue::CalculatorValue(bool value) {
//	this->type = new Type(BooleanType);
//	bool* newValue = new bool;
//	*newValue = value;
//	this->value = newValue;
//}

CalculatorValue::CalculatorValue(int value) {
	this->type = new Type(IntegerType);
	int* newValue = new int;
	*newValue = value;
	this->value = newValue;
}

//CalculatorValue::CalculatorValue(double value) {
//	this->type = new Type(RealType);
//	double* newValue = new double;
//	*newValue = value;
//	this->value = newValue;
//}

//CalculatorValue::CalculatorValue::CalculatorValue(char value) {
//	this->type = new Type(CharacterType);
//	char* newValue = new char;
//	*newValue = value;
//	this->value = newValue;
//}

//CalculatorValue::CalculatorValue(Vector<CalculatorValue>* value) {
//	if (value->getCount() > 0) {
//		this->type = new Type(VectorType);
////		, value->get(0)->getType()->getType()
//	} else {
//		this->type = new Type(VectorType);
//	}
//	this->value = new Vector<CalculatorValue>(*value);
//}

CalculatorValue::CalculatorValue(Type* type, void* value) {
	this->type = type;
	this->value = value;
}

CalculatorValue::CalculatorValue(const CalculatorValue& original) {
	this->type = original.type;
	
//	bool* newBool;
	int* newInt;
//	double* newDouble;
//	char* newChar;
//	Vector<CalculatorValue>* newVector;
	
	switch (this->type->getType()) {
		case NullType:
		case IdentityType:
			this->value = nullptr;
			break;
//		case BooleanType:
//			newBool = new bool;
//			*newBool = *(bool*)original.value;
//			this->value = newBool;
//			break;
		case IntegerType:
			newInt = new int;
			*newInt = *(int*)original.value;
			this->value = newInt;
			break;
//		case RealType:
//			newDouble = new double;
//			*newDouble = *(double*)original.value;
//			this->value = newDouble;
//			break;
//		case CharacterType:
//			newChar = new char;
//			*newChar = *(char*)original.value;
//			this->value = newChar;
//			break;
//		case IntervalType: // fallthrough
//		case VectorType: // fallthrough
//		case MatrixType: // fallthrough
//		case TupleType:
//			newVector = (Vector<CalculatorValue>*)original.value;
//			newVector = new Vector<CalculatorValue>(*newVector);
//			this->value = newVector;
//			break;
//			
		case StandardOut:
		case StandardIn:
//		case VectorStart:
			this->value = nullptr;
			break;
		case Lvalue:
			this->value = original.value;
	}
}

CalculatorValue::~CalculatorValue() {
	switch (this->type->getType()) {
		case NullType:
		case IdentityType:
			break;
//		case BooleanType:
//			delete this->booleanValue();
//			break;
		case IntegerType:
			delete this->integerValue();
			break;
//		case RealType:
//			delete this->realValue();
//			break;
//		case CharacterType:
//			delete this->characterValue();
//			break;
//		case IntervalType: // fallthrough
//		case VectorType: // fallthrough
//		case MatrixType: // fallthrough
//		case TupleType:
//			delete this->vectorValue();
//			break;
		case StandardOut: // fallthrough
		case StandardIn: // fallthrough
//		case VectorStart:
		case Lvalue:
			break;
	}
}

Type* CalculatorValue::getType() {
	return this->type;
}

bool CalculatorValue::isNull() {
	return this->type->getType() == NullType;
}

bool CalculatorValue::isIdentity() {
	return this->type->getType() == IdentityType;
}

//bool CalculatorValue::isBoolean() {
//	return this->type->getType() == BooleanType;
//}

bool CalculatorValue::isInteger() {
	return this->type->getType() == IntegerType;
}

//bool CalculatorValue::isReal() {
//	return this->type->getType() == RealType;
//}
//
//bool CalculatorValue::isCharacter() {
//	return this->type->getType() == CharacterType;
//}
//
//bool CalculatorValue::isInterval() {
//	return this->type->getType() == IntervalType;
//}
//
//bool CalculatorValue::isVector() {
//	return this->type->getType() == VectorType;
//}
//
//bool CalculatorValue::isMatrix() {
//	return this->type->getType() == MatrixType;
//}
//
//bool CalculatorValue::isTuple() {
//	return this->type->getType() == TupleType;
//}

bool CalculatorValue::isStandardOut() {
	return this->type->getType() == StandardOut;
}

bool CalculatorValue::isStandardIn() {
	return this->type->getType() == StandardIn;
}

//bool CalculatorValue::isVectorStart() {
//	return this->type->getType() == VectorStart;
//}

bool CalculatorValue::isLvalue() {
	return this->type->getType() == Lvalue;
}

//bool* CalculatorValue::booleanValue() {
//	if (!this->isBoolean()) { printf("Not a boolean\n"); exit(1); }
//	return (bool*)this->value;
//}

int* CalculatorValue::integerValue() {
	if (!this->isInteger()) { printf("Not a integer\n"); exit(1); }
	return (int*)this->value;
}

//double* CalculatorValue::realValue() {
//	if (!this->isReal()) { printf("Not a real \n"); exit(1); }
//	return (double*)this->value;
//}
//
//char* CalculatorValue::characterValue() {
//	if (!this->isCharacter()) { printf("Not a character"); exit(1); }
//	return (char*)this->value;
//}

CalculatorValue* CalculatorValue::lvalue() {
	if (!this->isLvalue()) { printf("Not a lvalue"); exit(1); }
	return *(CalculatorValue**)this->value;
}

CalculatorValue** CalculatorValue::lvalue_raw() {
	if (!this->isLvalue()) { printf("Not a lvalue"); exit(1); }
	return (CalculatorValue**)this->value;
}

//Vector<CalculatorValue>* CalculatorValue::intervalValue() {
//	if (!this->isInterval()) { printf("Not an interval"); exit(1); };
//	return (Vector<CalculatorValue>*)this->value;
//}
//
//Vector<CalculatorValue>* CalculatorValue::vectorValue() {
//	if (!this->isVector()) { printf("Not a vector"); exit(1); };
//	return (Vector<CalculatorValue>*)this->value;
//}
//
//Vector<CalculatorValue>* CalculatorValue::matrixValue() {
//	if (!this->isMatrix()) { printf("Not a matrix"); exit(1); };
//	return (Vector<CalculatorValue>*)this->value;
//}
//
//Vector<CalculatorValue>* CalculatorValue::tupleValue() {
//	if (!this->isTuple()) { printf("Not a tuple"); exit(1); };
//	return (Vector<CalculatorValue>*)this->value;
//}
