#pragma once

#include "notAllowedImports.h"

#include "allowedImports.h"
#include "Object.h"
#include "ValueType.h"
#include "Vector.h"

class Value : public Object {
public:
	Value(bool value) : super(TypeValue) {
		this->valueType = new ValueType(BooleanType);
		bool* newValue = new bool;
		*newValue = value;
		this->value = newValue;
	}
	
	Value(int value) : super(TypeValue) {
		this->valueType = new ValueType(IntegerType);
		int* newValue = new int;
		*newValue = value;
		this->value = newValue;
	}
	
	Value(float value) : super(TypeValue) {
		this->valueType = new ValueType(RealType);
		float* newValue = new float;
		*newValue = value;
		this->value = newValue;
	}
	
	Value(char value) : super(TypeValue) {
		this->valueType = new ValueType(CharacterType);
		char* newValue = new char;
		*newValue = value;
		this->value = newValue;
	}
	
	/// - Note: `value` is an exception as it becomes fully owned by `Value` -
	///         it is NOT retained and should not be released externally.
	Value(ValueType* type, void* value) : super(TypeValue) {
		type->retain();
		
		this->valueType = type;
		this->value = value;
	}
	
	virtual Value* copy() const {
		Value* copy = new Value(this->valueType, this->value);
		switch (this->valueType->getType()) {
			case NullType:
			case IdentityType:
			case StandardIn:
			case StandardOut:
				break;
			case BooleanType:
				copy->value = new bool;
				*(bool*)(copy->value) = *(bool*)this->value;
				break;
			case IntegerType:
				copy->value = new int;
				*(int*)(copy->value) = *(int*)this->value;
				break;
			case RealType:
				copy->value = new float;
				*(float*)(copy->value) = *(float*)this->value;
				break;
			case CharacterType:
				copy->value = new char;
				*(char*)(copy->value) = *(char*)this->value;
				break;
			case TupleType:
				copy->value = this->tupleValue()->copy();
				break;
			case IntervalType:
			    copy->value = this->intervalValue()->copy();
			    break;
			case VectorType:
			// TODO: copy
			    //copy->value = this->vectorValue();
			    break;
			case Lvalue:
				// TODO: Retain???
				break;
			case StartVector:
				// TODO: Retain???
				break;
		}
		return copy;
	}
	
	ValueType* getType() {
		this->valueType->retain();
		return this->valueType;
	}
	
	bool isNull()		const { return this->valueType->getType() == NullType;	}
	bool isIdentity()	const { return this->valueType->getType() == IdentityType;}
	bool isBoolean()	const { return this->valueType->getType() == BooleanType;	}
	bool isInteger()	const { return this->valueType->getType() == IntegerType;	}
	bool isReal()		const { return this->valueType->getType() == RealType;	}
	bool isCharacter()	const { return this->valueType->getType() == CharacterType;}
	bool isTuple()		const { return this->valueType->getType() == TupleType;   }
	bool isInterval()   const { return this->valueType->getType() == IntervalType; }
	bool isVector()     const { return this->valueType->getType() == VectorType; }
	bool isStandardIn()	const { return this->valueType->getType() == StandardIn;	}
	bool isStandardOut()const { return this->valueType->getType() == StandardOut;	}
	bool isLvalue()		const { return this->valueType->getType() == Lvalue;		}
	bool isStartVector()const { return this->valueType->getType() == StartVector; }

	
	bool* booleanValue() {
		bool* b = new bool;
		if (this->isNull())		{ *b = false; return b; }
		if (this->isIdentity())	{ *b = true;  return b; }
		if (!this->isBoolean()) { printf("Not a boolean value\n"); exit(1); }
		*b = *(bool*)this->value;
		return b;
	}
	
	int* integerValue() {
		int* i = new int;
		if (this->isNull())		{ *i = 0; return i; }
		if (this->isIdentity())	{ *i = 1; return i; }
		if (!this->isInteger())	{ printf("Not an integer value\n"); exit(1); }
		*i = *(int*)this->value;
		return i;
	}
	
	float* realValue() {
		float* r = new float;
		if (this->isNull())		{ *r = 0.0; return r;	}
		if (this->isIdentity())	{ *r = 1.0; return r;	}
		if (!this->isReal())	{ printf("Not a real value\n"); exit(1); }
		*r = *(float*)this->value;
		return r;
	}

	char* characterValue() {
		char* c = new char;
		if (this->isNull())		{ *c = 0; return c;	}
		if (this->isIdentity())	{ *c = 1; return c;	}
		if (!this->isCharacter()){ printf("Not a character value\n"); exit(1); }
		*c = *(char*)this->value;
		return c;
	}
	
	Vector<Value>* tupleValue() const {
		if (this->isNull())		{ printf("This is a null tuple\n"); exit(1); }
		if (this->isIdentity())	{ printf("This is an identity tuple\n"); exit(1); }
		if (!this->isTuple())	{ printf("Not a tuple value\n"); exit(1); }
		return ((Vector<Value>*)this->value)->copy();
	}

	Vector<Value>* intervalValue() const {
		if (this->isNull())		{ printf("This is a null interval\n"); exit(1); }
		if (this->isIdentity())	{ printf("This is an identity interval\n"); exit(1); }
		if (!this->isInterval()){ printf("Not an Interval\n"); exit(1); }
		return ((Vector<Value>*)this->value)->copy();
	}

    Vector<Value>* vectorValue() const {
        if (this->isNull())		{ printf("This is a null interval\n"); exit(1); }
        if (this->isIdentity())	{ printf("This is an identity interval\n"); exit(1); }
        if (!this->isVector()){ printf("Not a Vector \n"); exit(1); }
        return ((Vector<Value>*)this->value)->copy();
    }

	Value* lvalue() {
		if (!this->isLvalue()) { printf("Not an lvalue\n"); exit(1); }
		Value* value = *(Value**)this->value;
		value->retain();
		return value;
	}
	
	Value** lvalue_ptr() {
		if (!this->isLvalue()) { printf("Not an lvalue\n"); exit(1); }
		return (Value**)this->value;
	}
	
	void* value_ptr() {
		return this->value;
	}
	
private:
	ValueType* valueType;
	void* value;
	
	~Value() {
		switch (this->valueType->getType()) {
			case NullType:
			case IdentityType:
				break;
			case BooleanType:	delete (bool*)   this->value; break;
			case IntegerType:	delete (int*)    this->value; break;
			case RealType:		delete (float*)  this->value; break;
			case CharacterType:	delete (char*)	 this->value; break;
			case TupleType:     ((Vector<Value>*)this->value)->release(); break;
			case IntervalType:  ((Vector<Value>*)this->value)->release(); break;
			case VectorType:    ((Vector<Value>*) this->value)->release(); break;
			case StandardIn:
			case StandardOut:
			case Lvalue:
			case StartVector:
				break;
		}
		this->valueType->release();
	}
	
	typedef Object super;
};

