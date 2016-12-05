#pragma once

#include "literals.h"

void swapStackTopTwo() {
	Value* topValue = stack->pop();
	Value* secondValue = stack->pop();
	stack->push(topValue);
	stack->push(secondValue);
	topValue->release();
	secondValue->release();
}

void promoteTo_b() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	int* intValue = nullptr;
	char* charValue = nullptr;
	ValueType* type = value->getType();
    ValueType* newType = nullptr;
    Value* node = nullptr;
    int size = 0;
    Vector<Value>* vectorValues = nullptr;
	switch (type->getType()) {
		case NullType:
			newValue = new Value(false);
			break;
		case IdentityType:
			newValue = new Value(true);
			break;
		case BooleanType:
			newValue = value;
			newValue->retain();
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value(*intValue != 0);
			break;
		case RealType: printf("Cannot promote Real\n"); exit(1);
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value(*charValue != '\0');
			break;
        case VectorType:
            size = value->vectorValue()->getCount();
            vectorValues = new Vector<Value>;
            for (int i = 0; i < size; i++){
                node = value->vectorValue()->get(i);
                stack->push(node);
                promoteTo_b();
                node = stack->pop()->copy();
                vectorValues->append(node);
            }
            newType = new ValueType(VectorType);
            newValue = new Value(newType, vectorValues);
            break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case IntervalType: printf("Cannot promote Interval\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_b();
		case StartVector: printf("Cannot promote StartVector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_i() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	float* realValue = nullptr;
	char* charValue = nullptr;
	ValueType* type = value->getType();
    ValueType* newType = nullptr;
    Value* node = nullptr;
    int size = 0;
    Vector<Value>* vectorValues = nullptr;
	switch (type->getType()) {
		case NullType:
			newValue = new Value(0);
			break;
		case IdentityType:
			newValue = new Value(1);
			break;
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value(*boolValue ? 1 : 0);
			break;
		case IntegerType:
			newValue = value;
			newValue->retain();
			break;
		case RealType:
			realValue = value->realValue();
			newValue = new Value((int)*realValue);
			break;
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value((int)*charValue);
			break;
        case VectorType:
            size = value->vectorValue()->getCount();
            vectorValues = new Vector<Value>;
            for (int i = 0; i < size; i++){
                node = value->vectorValue()->get(i);
                stack->push(node);
                promoteTo_i();
                node = stack->pop()->copy();
                vectorValues->append(node);
            }
            newType = new ValueType(VectorType);
            newValue = new Value(newType, vectorValues);
            break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case IntervalType: printf("Cannot promote Interval\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_i();
		case StartVector: printf("Cannot promote startvector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_r() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	int* intValue = nullptr;
	char* charValue = nullptr;
	ValueType* type = value->getType();
	ValueType* newType = nullptr;
	Value* node = nullptr;
	int size = 0;
	Vector<Value>* vectorValues = nullptr;
	switch (type->getType()) {
		case NullType:
			newValue = new Value(0.0f);
			break;
		case IdentityType:
			newValue = new Value(1.0f);
			break;
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value(*boolValue ? 1.0f : 0.0f);
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value((float)*intValue);
			break;
		case RealType:
			newValue = value;
			newValue->retain();
			break;
		case CharacterType:
			charValue = value->characterValue();
			newValue = new Value((float)*charValue);
			break;
        case VectorType:
            size = value->vectorValue()->getCount();
            vectorValues = new Vector<Value>;
            for (int i = 0; i < size; i++){
                node = value->vectorValue()->get(i);
                stack->push(node);
                promoteTo_r();
                node = stack->pop()->copy();
                vectorValues->append(node);
            }
            newType = new ValueType(VectorType);
            newValue = new Value(newType, vectorValues);
            break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case IntervalType: printf("Cannot promote Interval\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_r();
		case StartVector: printf("Cannot promote StartVector"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteTo_c() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	int* intValue = nullptr;
	ValueType* type = value->getType();
    ValueType* newType = nullptr;
    Value* node = nullptr;
    int size = 0;
    Vector<Value>* vectorValues = nullptr;
	switch (type->getType()) {
		case NullType:
			newValue = new Value('\0');
			break;
		case IdentityType:
			newValue = new Value('\1');
			break;
		case BooleanType:
			boolValue = value->booleanValue();
			newValue = new Value((char)(*boolValue ? 1 : 0));
			break;
		case IntegerType:
			intValue = value->integerValue();
			newValue = new Value((char)(*intValue % 256));
			break;
		case RealType: printf("Cannot promote real\n"); exit(1);
		case CharacterType:
			newValue = value;
			newValue->retain();
			break;
        case VectorType:
            size = value->vectorValue()->getCount();
            vectorValues = new Vector<Value>;
            for (int i = 0; i < size; i++){
                node = value->vectorValue()->get(i);
                stack->push(node);
                promoteTo_c();
                node = stack->pop()->copy();
                vectorValues->append(node);
            }
            newType = new ValueType(VectorType);
            newValue = new Value(newType, vectorValues);
            break;
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
	    case IntervalType: printf("Cannot promote Interval\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_c();
		case StartVector: printf("Cannot promote StartVector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

// promotion of interval
void promoteTo_l() {
    Value* value = stack->pop();
    ValueType* type = value->getType();
	Value* newValue = nullptr;
    switch (type->getType()) {
	    case IntervalType:
			newValue = value;
			newValue->retain();
			break;
        case NullType: printf("Cannot promote NullType\n"); exit(1);
		case IdentityType: printf("Cannot promote IdentityType\n"); exit(1);
		case BooleanType: printf("Cannot promote BooleanType\n"); exit(1);
		case IntegerType: printf("Cannot promote IntegerType\n"); exit(1);
		case RealType: printf("Cannot promote real\n"); exit(1);
		case CharacterType: printf("Cannot promote CharacterType\n"); exit(1);
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
	    case VectorType: printf("Cannot promote Vector\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
            newValue = value->lvalue();
            stack->push(newValue);
            newValue->release();
            return promoteTo_l();
		case StartVector: printf("Cannot promote StartVector\n"); exit(1);
    }
    type->release();
    stack->push(newValue);
    newValue->release();
    value->release();
}

// use this for vector promotion and declaration
void pushVectorValueType(int size, char charType) {
    ValueType *type = new ValueType(VectorType);
    type->setVectorSize(size);
    switch(charType) {
        case 'r':
            type->setContainedType(RealType);
            break;
        case 'i':
            type->setContainedType(IntegerType);
            break;
        case 'c':
            type->setContainedType(CharacterType);
            break;
        case 'b':
            type->setContainedType(BooleanType);
            break;
        default:
            throw "cannot make vector with containing type specified";
    }

  	Value* value = new Value(type, nullptr);
    stack->push(value);
}

// null promotion
void promoteTo_n() {
    Value *value = stack->pop();
    ValueType *type = value->getType();
    Value *newValue = nullptr;
    switch(type->getType()) {
        case IntervalType: printf("Cannot promote IntervalType\n"); exit(1);
        case NullType:
            newValue = value;
            newValue->retain();
            break;
        case IdentityType: printf("Cannot promote IdentityType\n"); exit(1);
        case BooleanType: printf("Cannot promote BooleanType\n"); exit(1);
        case IntegerType: printf("Cannot promote IntegerType\n"); exit(1);
        case RealType: printf("Cannot promote real\n"); exit(1);
        case CharacterType: printf("Cannot promote CharacterType\n"); exit(1);
        case TupleType: printf("Cannot promote Tuple\n"); exit(1);
        case VectorType: printf("Cannot promote Vector\n"); exit(1);
        case StandardOut: printf("Cannot promote stdout\n"); exit(1);
        case StandardIn: printf("Cannot promote stdin\n"); exit(1);
        case Lvalue:
            newValue = value->lvalue();
            stack->push(newValue);
            newValue->release();
            return promoteTo_l();
       	case StartVector: printf("Cannot promote StartVector\n"); exit(1);
    }

    type->release();
    stack->push(newValue);
    newValue->release();
    value->release();
}

// identity promotion
void promoteTo_d() {
    Value *value = stack->pop();
    ValueType *type = value->getType();
    Value *newValue = nullptr;
    switch(type->getType()) {
        case IntervalType: printf("Cannot promote IntervalType\n"); exit(1);
        case NullType: printf("Cannot promote NullType\n"); exit(1);
        case IdentityType:
            newValue = value;
            newValue->retain();
            break;
        case BooleanType: printf("Cannot promote BooleanType\n"); exit(1);
        case IntegerType: printf("Cannot promote IntegerType\n"); exit(1);
        case RealType: printf("Cannot promote real\n"); exit(1);
        case CharacterType: printf("Cannot promote CharacterType\n"); exit(1);
        case TupleType: printf("Cannot promote Tuple\n"); exit(1);
        case VectorType: printf("Cannot promote Vector\n"); exit(1);
        case StandardOut: printf("Cannot promote stdout\n"); exit(1);
        case StandardIn: printf("Cannot promote stdin\n"); exit(1);
        case Lvalue:
            newValue = value->lvalue();
            stack->push(newValue);
            newValue->release();
            return promoteTo_l();
        case StartVector: printf("Cannot promote StartVector\n"); exit(1);
    }

    type->release();
    stack->push(newValue);
    newValue->release();
    value->release();
}

void padVectorToStrictSize() {
    _unwrap();

    Value *vectorValue = stack->pop();
    if (! vectorValue->isVector()) {
        printf("we require a vector for padVectorToStrictSize");
        exit(1);
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
                printf("contained type invalid\n");
                exit(1);
        }

        vector->append(toPush);
    }

    stack->push(vectorValue);
}

// requires a reference Vector (e.g. pushVectorValueType) on stack to compare
// the dimensions and type to
void promoteTo_v() {
	Value* value = stack->pop();
	Value* newValue = nullptr;
	bool* boolValue = nullptr;
	int* intValue = nullptr;
	Vector<Value>* vectorValues = nullptr;
	ValueType* newType = nullptr;
	ValueType* type = value->getType();
	Vector<Value>* intervalValues = nullptr;
	Value* node = nullptr;
	int start = 0;
	int end = 0;
	switch (type->getType()) {
        case IntervalType:
            intervalValues = value->intervalValue();
            stack->push(intervalValues->get(0));
            _unwrap();
            start = *(stack->pop()->integerValue());
            stack->push(intervalValues->get(1));
            _unwrap();
            end = *(stack->pop()->integerValue());

            vectorValues = new Vector<Value>;

            for (int i = start; i <= end; i++){
                node = new Value(i);
                vectorValues->append(node);
            }

           	newType = new ValueType(VectorType);
           	newType->setContainedType(IntegerType);
           	newType->setVectorSize(end-start + 1);
           	newValue = new Value(newType, vectorValues);

            break;
        case VectorType:
			newValue = value;
			newValue->retain();
			break;
		case BooleanType: printf("Cannot promote BooleanType\n"); exit(1);
		case IntegerType: printf("Cannot promote IntegerType\n"); exit(1);
		case RealType: printf("Cannot promote real\n"); exit(1);
		case CharacterType: printf("Cannot promote CharacterType\n"); exit(1);
        case NullType: printf("Cannot promote NullType\n"); exit(1);
		case IdentityType: printf("Cannot promote IdentityType\n"); exit(1);
		case TupleType: printf("Cannot promote Tuple\n"); exit(1);
		case StandardOut: printf("Cannot promote stdout\n"); exit(1);
		case StandardIn: printf("Cannot promote stdin\n"); exit(1);
		case Lvalue:
			newValue = value->lvalue();
			stack->push(newValue);
			newValue->release();
			return promoteTo_v();
		case StartVector: printf("Cannot promote StartVector\n"); exit(1);
	}
	type->release();
	stack->push(newValue);
	newValue->release();
	value->release();
}

void promoteVector(char cType) {
    _unwrap();

    Value* toSizeValue = stack->pop();
    int *toSize = toSizeValue->integerValue();

	Value* poppedValue = stack->pop();
    Vector<Value>* poppedVector;

	if (poppedValue->getType()->getType() != VectorType) {
        int index;
	    switch (poppedValue->getType()->getType()) {
	        case IdentityType:
	            poppedVector = new Vector<Value>();
	            if (toSize < 0) { printf("can't play this identity game with no size\n"); exit(1); }
	            poppedValue = new Value(new ValueType(VectorType), poppedVector);
	            for (index = 0; index < *toSize; ++index) {
	                poppedVector->append(new Value(new ValueType(IdentityType), nullptr));
	            }
	            break;
	        case NullType:
	            poppedValue = new Value(new ValueType(VectorType), new Vector<Value>);
	            break;
	        case IntervalType:
	            stack->push(poppedValue);
	            promoteTo_v();
	            poppedValue = stack->pop();
                break;
	        default:
	            printf("promoteVector cannot promote this type\n");
	            exit(1);
	    }
	}

	poppedVector = poppedValue->vectorValue();
	int poppedSize = poppedVector->getCount();
    int goalSize = (*toSize) == -1 ? poppedSize : (*toSize);

    ValueType* newValueType = new ValueType(VectorType);
	Value* newValue = new Value(newValueType, new Vector<Value>());
    Vector<Value>* newVector = newValue->vectorValue();

    newValueType->setVectorSize(goalSize);
    switch (cType) {
        case 'b': newValueType->setContainedType(BooleanType); break;
        case 'c': newValueType->setContainedType(CharacterType); break;
        case 'i': newValueType->setContainedType(IntegerType); break;
        case 'r': newValueType->setContainedType(RealType); break;
        default: throw "cannot promote vector to this type"; break;
    }

    Value* indexedValue = nullptr;

    for (int index = 0; index < poppedSize; ++index) {
        indexedValue = poppedVector->get(index);
        stack->push(indexedValue);
        switch (cType) {
            case 'b': promoteTo_b(); break;
            case 'c': promoteTo_c(); break;
            case 'i': promoteTo_i(); break;
            case 'r': promoteTo_r(); break;
            default: throw "cannot promote vector to this type"; break;
        }
        indexedValue = stack->pop();
        newVector->append(indexedValue);

    }

	stack->push(newValue);
    padVectorToStrictSize();
}



// requires a reference tuple which it will consume
void promoteTuple() {
    Value* toPromoteValue = stack->pop();

    switch (toPromoteValue->getType()->getType()) {
        case NullType:
            pushNullTuple();
            swapStackTopTwo();
            popStack();
            return;
        case IdentityType:
            pushNullTuple();
            swapStackTopTwo();
            popStack();
            return;
        case TupleType:
            // move along.
            break;
        default:
            printf("Cannot cast this type as a tuple\n"); exit(1);
    }

    Vector<Value>* toPromote = toPromoteValue->tupleValue();
    Value* refValue = stack->pop();
    Vector<Value>* ref = refValue->tupleValue();

    Value* returnedValue = new Value(new ValueType(TupleType), new Vector<Value>);
    Vector<Value>* returnedTuple = returnedValue->tupleValue();

    int refSize = ref->getCount();
    int toPromoteSize = toPromote->getCount();

    if (refSize != toPromoteSize) {
        printf("Sizes are unequal\n");
        exit(1);
    }

    for (int i = 0; i < refSize; ++i) {
        Value* refNode = ref->get(i);
        Value* toPromoteNode = toPromote->get(i);

        stack->push(toPromoteNode);
        switch(refNode->getType()->getType()) {
            case BooleanType:
                promoteTo_b();
                break;
            case CharacterType:
                promoteTo_c();
                break;
            case IntegerType:
                promoteTo_i();
                break;
            case RealType:
                promoteTo_r();
                break;
            case VectorType:
                stack->push(toPromoteNode);
                stack->push(new Value(refNode->getType()->getVectorSize()));
                switch (refNode->getType()->getContainedType()) {
                    case BooleanType:
                        promoteVector('b');
                        break;
                    case CharacterType:
                        promoteVector('c');
                        break;
                    case IntegerType:
                        promoteVector('i');
                        break;
                    case RealType:
                        promoteVector('r');
                        break;
                    default:
                        printf("vector cannot be promoted to this type.\n"); exit(1);
                }
                break;
            default: printf("invalid reference node of tuple\n"); exit(1);
        }
        toPromoteNode = stack->pop();
        returnedTuple->append(toPromoteNode);
    }

    stack->push(returnedValue);
}
