#pragma once

#include "BuiltinType.h"

void pushEmptyValue(BuiltinType builtinType) {
	ValueType* type = new ValueType(builtinType);
	Value* value = new Value(type, nullptr);
	stack->push(value);
	type->release();
	value->release();
}

void pushNull()		{ pushEmptyValue(NullType);		}
void pushIdentity()	{ pushEmptyValue(IdentityType);	}

void pushBoolean(bool value) {
	Value* booleanValue = new Value(value);
	stack->push(booleanValue);
	booleanValue->release();
}

void varInitPushNullBoolean() {
	pushBoolean(false);
}

void pushInteger(int value) {
	Value* integerValue = new Value(value);
	stack->push(integerValue);
	integerValue->release();
}

void varInitPushNullInteger() {
	pushInteger(0);
}

void pushReal(float value) {
	Value* floatValue = new Value(value);
	stack->push(floatValue);
	floatValue->release();
}

void varInitPushNullReal() {
	pushReal(0.0);
}

void pushCharacter(char value) {
	Value* charValue = new Value(value);
	stack->push(charValue);
	charValue->release();
}

void varInitPushNullCharacter() {
	pushReal('\0');
}

void pushStartVector() {
	ValueType* type = new ValueType(StartVector);
	Value* startVector = new Value(type, nullptr);
	stack->push(startVector);
	startVector->release();
	type->release();
}

void endInterval() {
    Value * node1 = stack->pop();
    Value * node2 = stack->pop();

    Vector<Value>* intervalValues = new Vector<Value>;

    intervalValues->append(node1);
    intervalValues->append(node2);

    ValueType* type = new ValueType(IntervalType);

    Value* interval = new Value(type, intervalValues);

    stack -> push(interval);

    interval -> release();
    type -> release();
}

void endVector() {
    Stack<Value>* elements = new Stack<Value>;

    Value* element = stack->pop();
    while (!element->isStartVector()) {
        elements->push(element);
        element->release();
        element = stack->pop();
    }
    element->release();

    Vector<Value>* vectorValues = new Vector<Value>;

    Value* node = elements->pop();
    while (node != nullptr) {
        vectorValues->append(node);
        node->release();
        node = elements->popOrNull();
    }

    ValueType* type = new ValueType(VectorType);
    Value* vector = new Value(type, vectorValues);
    stack->push(vector);
    type->release();
    vector->release();
    elements->release();
}

void endTuple() {
	Stack<Value>* elements = new Stack<Value>;
	
	Value* element = stack->pop();
	while (!element->isStartVector()) {
		elements->push(element);
		element->release();
		element = stack->pop();
	}
	element->release();
	
	Vector<Value>* tupleValues = new Vector<Value>;
	
	Value* node = elements->pop();
	while (node != nullptr) {
		tupleValues->append(node);
		node->release();
		node = elements->popOrNull();
	}
	
	ValueType* type = new ValueType(TupleType);
	Value* tuple = new Value(type, tupleValues);
	stack->push(tuple);
	type->release();
	tuple->release();
	elements->release();
	//tupleValues->release(); - Do not release
}

void setTuple(int i) {
    Value *value1 = stack->pop();
    Value* value = stack->pop(); // this is the tuple

    	if (!value->isLvalue()) {
    		printf("Not an lvalue");
    		exit(1);
    	}
    	Value** ptr = value->lvalue_ptr();
    	if (!(*ptr)->isTuple()) {
    		printf("NOT A VECTOR OR TUPLE\n");
    		exit(1);
    	}

    ((Vector<Value>*)(*ptr)->value_ptr())->set(i, value1);

    Vector<Value>* tupleValues = new Vector<Value>;

    int index = ((Vector<Value>*)(*ptr)->value_ptr())->getCount();

	Value* node;
    for (int j = 0; j < index; ++j) {
        node = ((Vector<Value>*)(*ptr)->value_ptr())->get(j);
    	tupleValues->append(node);
    }

	ValueType* type = new ValueType(TupleType);

    Value* tuple = new Value(type, tupleValues);

    stack -> push(tuple);

    type->release();
    tuple->release();
    value->release();
    value1->release();
}