#pragma once

void push(void** variable) {
	ValueType* type = new ValueType(Lvalue);
	Value* value = new Value(type, variable);
	stack->push(value);
	type->release();
	value->release();
}

void pushVariableValue(void** variable) {
	Value* value = *(Value**)variable;
	stack->push(value);
	value->release();
}

void assign(void** variable) {
	Value** var = (Value**)variable;
	Value* rvalue = stack->pop();
	
	// Transform the rvalue to the default value when using `null`
	if (rvalue->isNull()) {
	    int varSize;
	    int dex;
	    Value *v;
		rvalue->release();
		ValueType* rType = nullptr;
		switch ((*var)->getType()->getType()) {
			case NullType:
			case IdentityType:
				printf("Cannot determine type");
				exit(1);
				break;
			case BooleanType:	rvalue = new Value(false);		break;
			case IntegerType:	rvalue = new Value(0);			break;
			case RealType:		rvalue = new Value(0.0f);		break;
			case CharacterType:	rvalue = new Value((char)0);	break;
			case TupleType:
			    rvalue = new Value(new Vector<Value>());
			    rvalue = new Value(new Vector<Value>());
                varSize = (*var)->tupleValue()->getCount();
                dex = 0;
                while(dex < varSize) {
                    v = (*var)->tupleValue()->get(dex);
                    switch(v->getType()->getType()) {
                        case NullType:
                        case IdentityType:
                        case TupleType:
                        case StandardOut:
                        case StandardIn:
                        case Lvalue:
                            printf("Tuple cannot contain this type\n");
                            exit(1);
                            break;
                        case BooleanType:	rvalue->tupleValue()->append(new Value(false));		break;
                        case IntegerType:	rvalue->tupleValue()->append(new Value(0));			break;
                        case RealType:		rvalue->tupleValue()->append(new Value(0.0f));		break;
                        case CharacterType:	rvalue->tupleValue()->append(new Value((char)0));	break;
                    }
                    ++dex;
                }
                break;
			case StandardOut:
				rType = new ValueType(StandardOut);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case StandardIn:
				rType = new ValueType(StandardIn);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case Lvalue:
				printf("Cannot lvalue");
				exit(1);
				break;
		}
	}

	// Transform the rvalue to the default value when using `identity`
	if (rvalue->isIdentity()) {
	    Value *v;
	    int varSize;
	    int dex;
		rvalue->release();
		ValueType* rType = nullptr;
		switch ((*var)->getType()->getType()) {
			case NullType:
			case IdentityType:
				printf("Cannot determine type");
				exit(1);
				break;
			case BooleanType:	rvalue = new Value(true);		break;
			case IntegerType:	rvalue = new Value(1);			break;
			case RealType:		rvalue = new Value(1.0f);		break;
			case CharacterType:	rvalue = new Value((char)1);	break;
			case TupleType:
                rvalue = new Value(new Vector<Value>());
                varSize = (*var)->tupleValue()->getCount();
                dex = 0;
                while(dex < varSize) {
                    v = (*var)->tupleValue()->get(dex);
                    switch(v->getType()->getType()) {
                        case NullType:
                        case IdentityType:
                        case TupleType:
                        case StandardOut:
                        case StandardIn:
                        case Lvalue:
                            printf("Tuple cannot contain this type\n");
                            exit(1);
                            break;
                        case BooleanType:	rvalue->tupleValue()->append(new Value(true));		break;
                        case IntegerType:	rvalue->tupleValue()->append(new Value(1));			break;
                        case RealType:		rvalue->tupleValue()->append(new Value(1.0f));		break;
                        case CharacterType:	rvalue->tupleValue()->append(new Value((char)1));	break;
                    }
                    ++dex;
                }
                break;
			case StandardOut:
				rType = new ValueType(StandardOut);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case StandardIn:
				rType = new ValueType(StandardIn);
				rvalue = new Value(rType, nullptr);
				rType->release();
				break;
			case Lvalue:
				printf("Cannot lvalue");
				exit(1);
				break;
		}
	}

	if (*var != nullptr) { (*var)->release(); }
	if (rvalue->isLvalue()) {
		Value* newValue = rvalue->lvalue();
		newValue->retain();
		*var = newValue;
	} else {
		*var = rvalue;
	}
}
