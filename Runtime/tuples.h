#pragma once

#include "BuiltinType.h"
#include "declarations.h"

void getValueAtTuple(int index) {
    Value *tuple = stack->pop();
    Vector<Value> *tupleVal = tuple->tupleValue();
    Value *value = tupleVal->get(index);
    stack->push(value);
    tuple->release();
    tupleVal->release();
    value->release();
}

void assignTupleField(int index) {
    Value *tupleValue = stack->pop();
    Vector<Value> *tuple = tupleValue->tupleValue();
    Value *exprValue = stack->pop();

    tupleValue->set(index, value);


}

void pushIntNullToTuple() {
    Value *val = stack->pop();
    val->tupleValue()->append(new Value((int)0));
    stack->push(val);
}

void pushRealNullToTuple() {
    Value *val = stack->pop();
    val->tupleValue()->append(new Value((float)0));
    stack->push(val);
}

void pushCharNullToTuple() {
    Value *val = stack->pop();
    val->tupleValue()->append(new Value((char)0));
    stack->push(val);
}

void pushBoolNullToTuple() {
    Value *val = stack->pop();
    val->tupleValue()->append(new Value(false));
    stack->push(val);
}