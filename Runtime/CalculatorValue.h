#pragma once

class CalculatorValue {
private:
	Type* type;
	void* value;
	
public:
//	CalculatorValue(bool value);
	CalculatorValue(int value);
//	CalculatorValue(double value);
//	CalculatorValue(char value);
	/// free value manually
//	CalculatorValue(Vector<CalculatorValue>* value);
	/// do not free value
	CalculatorValue(Type* type, void* value);
	CalculatorValue(const CalculatorValue& original);
	~CalculatorValue();
	
	Type* getType();
	
	bool isNull();
	bool isIdentity();
//	bool isBoolean();
	bool isInteger();
//	bool isReal();
//	bool isCharacter();
//	bool isInterval();
//	bool isVector();
//	bool isMatrix();
//	bool isTuple();
	bool isStandardOut();
	bool isStandardIn();
//	bool isVectorStart();
	bool isLvalue();
	
//	bool* booleanValue();
	int* integerValue();
//	double* realValue();
//	char* characterValue();
	CalculatorValue* lvalue();
	CalculatorValue** lvalue_raw();
	
	/// free manually
//	Vector<CalculatorValue>* intervalValue();
//	Vector<CalculatorValue>* vectorValue();
//	Vector<CalculatorValue>* matrixValue();
//	Vector<CalculatorValue>* tupleValue();
};
