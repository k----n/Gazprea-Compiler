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

void shrinkIterateVector() {
    _unwrap();
    Value* value = stack->pop(); // this is the vector
    if (!(value->isVector())) {
        // possibly risky, just push back on false and return
        if (value->isStartVector()){
            pushStartVector();
            return;
        }

        printf("NOT A VECTOR\n");
        exit(1);
    }

    Vector<Value>* values = value->vectorValue();

    int size = values -> getCount();

    if (size == 1){
        pushStartVector();
        Value * element = values -> get(0);
        stack -> push(element);
        stack -> push(element);
    }
    else {
        Value * element = values -> get(0);
        Vector<Value>* smallerValues = new Vector<Value>;
        Value* node;
        for (int i = 1; i < size; i++){
            node = values -> get(i);
            smallerValues->append(node);
        }

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, smallerValues);

        stack -> push(newValue);
        stack -> push(element);
        stack -> push(element);
    }
}

// only pushes element once
void shrinkIterateVectorGen() {
    _unwrap();
    Value* value = stack->pop(); // this is the vector
    if (!(value->isVector())) {
        // possibly risky, just push back on false and return
        if (value->isStartVector()){
            pushStartVector();
            return;
        }

        printf("NOT A VECTOR\n");
        exit(1);
    }

    Vector<Value>* values = value->vectorValue();

    int size = values -> getCount();

    if (size == 1){
        pushStartVector();
        Value * element = values -> get(0);
        stack -> push(element);
    }
    else {
        Value * element = values -> get(0);
        Vector<Value>* smallerValues = new Vector<Value>;
        Value* node;
        for (int i = 1; i < size; i++){
            node = values -> get(i);
            smallerValues->append(node);
        }

        ValueType* newType = new ValueType(VectorType);
        Value* newValue = new Value(newType, smallerValues);

        stack -> push(newValue);
        stack -> push(element);
    }
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

    _unwrap();
    Value* element = stack->pop();

    int size = 0;
    while (!element->isStartVector()) {
        _unwrap();
        elements->push(element);
        element->release();
        element = stack->pop();
        ++size;
    }
    element->release();

    Vector<Value>* vectorValues = new Vector<Value>();

    Value* node = elements->popOrNull();
    while (node != nullptr) {
        vectorValues->append(node);
        node->release();
        node = elements->popOrNull();
    }

    ValueType* type = new ValueType(VectorType);
	type->setVectorSize(size);
    Value* vector = new Value(type, vectorValues);
    stack->push(vector);
    type->release();
    vector->release();
    elements->release();
}

void setVectorSize() {
    _unwrap();
    Value *valSizeData = stack->pop();
    int *size_data = valSizeData->integerValue();
    Value *vector = stack->pop();

    vector->getType()->setVectorSize(*size_data);

    stack->push(vector);
}

void padVectorToStrictSize() {
    _unwrap();

    Value *vectorValue = stack->pop();
    if (! vectorValue->isVector()) {
        throw "we require a vector for padVectorToStrictSize";
    }
    Vector<Value> *vector = vectorValue->vectorValue();
    ValueType *valueType = vectorValue->getType();

    int current_size = vector->getCount();

    int goal_size;
    if (valueType->hasVectorSize()) {
        goal_size = valueType->getVectorSize();
    } else {
        /* TODO: handle case where there is no goal size
            perhaps return is sufficient*/
        stack->push(vectorValue);
        return;
    }

    BuiltinType containedType;
    if (valueType->hasContainedType()) {
        containedType = valueType->getContainedType();
    } else {
        containedType = NullType;
    }

    for (int s = current_size; s < goal_size; ++s) {
        Value *toPush;

        switch (containedType) {
            case BooleanType:
                toPush = new Value((bool)0);
                break;
            case CharacterType:
                toPush = new Value((char)0);
                break;
            case IntegerType:
                toPush = new Value((int)0);
                break;
            case RealType:
                toPush = new Value((float)0.0);
                break;
            case IdentityType:
                toPush = new Value(new ValueType(IdentityType), nullptr);
                break;
            case NullType:
            	toPush = new Value(new ValueType(NullType), nullptr);
            	break;
            default:
                throw "contained type invalid";
        }

        vector->append(toPush);
    }

    stack->push(vectorValue);
}

void setVectorContainedType(char type) {
    Value *vector = stack->pop();

    switch (type) {
        case 'r':
            vector->getType()->setContainedType(RealType);
            break;
        case 'b':
            vector->getType()->setContainedType(BooleanType);
            break;
        case 'i':
            vector->getType()->setContainedType(IntegerType);
            break;
        case 'c':
            vector->getType()->setContainedType(CharacterType);
            break;
        case 'n':
            vector->getType()->setContainedType(NullType);
            break;
        case 'd':
            vector->getType()->setContainedType(IdentityType);
            break;
        default:
            throw "Vector cannot be of this char type";
    }

    stack->push(vector);
}

void pushIdentityVector(char cType) {
    Value* sizeValue = stack->pop();
    int size = *(sizeValue->integerValue());
    if (size == -1) {
        throw "Cannot push unsized null";
    }

    ValueType* newValueType = new ValueType(VectorType);
    newValueType->setVectorSize(size);
    switch(cType) {
        case 'b': newValueType->setContainedType(BooleanType); break;
        case 'c': newValueType->setContainedType(CharacterType); break;
        case 'i': newValueType->setContainedType(IntegerType); break;
        case 'r': newValueType->setContainedType(RealType); break;
        default: throw "vector cannot contain this type as a char"; break;
    }
    Value* newValue = new Value(newValueType, new Vector<Value>);
    Vector<Value>* newVector = newValue->vectorValue();

    for (int i = 0; i < size; ++i) {
        switch(cType) {
            case 'b': newVector->append(new Value(true)); break;
            case 'c': newVector->append(new Value((char)1)); break;
            case 'i': newVector->append(new Value((int)1)); break;
            case 'r': newVector->append(new Value((float)1)); break;
            default: throw "vector cannot contain this type as a char"; break;
        }
    }

    stack->push(newValue);
    newValue->release();
    newVector->release();
    newValueType->release();
    sizeValue->release();
}

void pushNullVector(char cType) {
    Value* sizeValue = stack->pop();
    int size = *(sizeValue->integerValue());
    if (size == -1) {
        throw "Cannot push unsized null";
    }

    ValueType* newValueType = new ValueType(VectorType);
    newValueType->setVectorSize(size);
    switch(cType) {
        case 'b': newValueType->setContainedType(BooleanType); break;
        case 'c': newValueType->setContainedType(CharacterType); break;
        case 'i': newValueType->setContainedType(IntegerType); break;
        case 'r': newValueType->setContainedType(RealType); break;
        default: throw "vector cannot contain this type as a char"; break;
    }
    Value* newValue = new Value(newValueType, new Vector<Value>);
    Vector<Value>* newVector = newValue->vectorValue();

    for (int i = 0; i < size; ++i) {
        switch(cType) {
            case 'b': newVector->append(new Value(false)); break;
            case 'c': newVector->append(new Value((char)0)); break;
            case 'i': newVector->append(new Value((int)0)); break;
            case 'r': newVector->append(new Value((float)0)); break;
            default: throw "vector cannot contain this type as a char"; break;
        }

    }
    stack->push(newValue);

}

void matchVectorSizes() {
    Value *value1 = stack->pop();
    Value *value2 = stack->pop();
    ValueType *value1type = value1->getType();
    ValueType *value2type = value2->getType();
    int value1size = -1;
    int value2size = -1;

    if (value1type->hasVectorSize()) {
        value1size = value1type->getVectorSize();
    } else {
        value1size = -1;
    }

    if (value2type->hasVectorSize()) {
        value2size = value2type->getVectorSize();
    } else {
        value2size = -1;
    }

    if (value1size < value2size) {
        value1type->setVectorSize(value2size);
    } else {
        value2type->setVectorSize(value1size);
    }

    stack->push(value2);
    stack->push(value1);
}

void matchVectorTypes() {
    Value *value1 = stack->pop();
    Value *value2 = stack->pop();
    ValueType *value1type = value1->getType();
    ValueType *value2type = value2->getType();
    BuiltinType value1containedType = value1type->getContainedType();
    BuiltinType value2containedType = value2type->getContainedType();

    if (value1containedType == NullType || value2containedType == RealType) {
        value1type->setContainedType(value2containedType);
    } else {
        value2type->setContainedType(value1containedType);
    }

    stack->push(value2);
    stack->push(value1);
}

void endTuple() {
    _unwrap();

	Stack<Value>* elements = new Stack<Value>;
	Value* element = stack->pop();

	while (!element->isStartVector()) {
        _unwrap();
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
    Value* value1 = stack->pop()->copy();
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