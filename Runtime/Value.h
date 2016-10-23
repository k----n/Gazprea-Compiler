#pragma once

#include "notAllowedImports.h"

#include "allowedImports.h"
#include "Object.h"
#include "ValueType.h"

class Value : public Object {
public:
	Value(int value) : super(TypeValue) {
		this->valueType = new ValueType(IntegerType);
		int* newValue = new int;
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
		this->valueType->retain();
		switch (this->valueType->getType()) {
			case NullType:
			case IdentityType:
			case IntegerType:
			case StandardIn:
			case StandardOut:
				break;
			case Lvalue:
				// TODO: Retain???
				break;
		}
		return copy;
	}
	
	ValueType* getType() {
		this->valueType->retain();
		return this->valueType;
	}
	
	bool isNull()		{ return this->valueType->getType() == NullType;	}
	bool isIdentity()	{ return this->valueType->getType() == IdentityType;}
	bool isInteger()	{ return this->valueType->getType() == IntegerType;	}
	bool isStandardIn()	{ return this->valueType->getType() == StandardIn;	}
	bool isStandardOut(){ return this->valueType->getType() == StandardOut;	}
	bool isLvalue()		{ return this->valueType->getType() == Lvalue;		}
	
	int* integerValue() {
		int* i = new int;
		if (this->isNull())		{ *i = 0; return i; }
		if (this->isIdentity())	{ *i = 1; return i; }
		if (!this->isInteger())	{ printf("Not an integer value\n"); exit(1); }
		*i = *(int*)this->value;
		return i;
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
	
private:
	ValueType* valueType;
	void* value;
	
	~Value() {
		this->valueType->release();
		switch (this->valueType->getType()) {
			case NullType:
			case IdentityType:
				break;
			case IntegerType:
				delete this->integerValue();
			case StandardIn:
			case StandardOut:
			case Lvalue:
				break;
		}
	}
	
	typedef Object super;
};
