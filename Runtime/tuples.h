#pragma once

#include "BuiltinType.h"
#include "declarations.h"

void getValueAtTuple(int index) {
    Value *tuple = stack->pop();
    stack->push(tuple->tupleValue()->get(index));
}

void assignTupleField() {
    Value *tupleField = stack->pop();
    Value *exprValue = stack->pop();

    tupleField->setValue(exprValue->getValue());
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