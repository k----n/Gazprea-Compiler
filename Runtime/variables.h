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
		rvalue->release();
		ValueType* rType = nullptr;
		int varSize;
		int index;
		Value* innerValue = nullptr;
		Vector<Value>* tupleValue = nullptr;
		Vector<Value>* intervalValue = nullptr;
		ValueType* valueType = (*var)->getType();
		ValueType* innerValueType = nullptr;
		Vector<Value>* rvalueVector = nullptr;
		switch (valueType->getType()) {
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
				rType = new ValueType(TupleType);
			    rvalue = new Value(rType, new Vector<Value>());
				tupleValue = (*var)->tupleValue();
                varSize = tupleValue->getCount();
                index = 0;
                while(index < varSize) {
                    innerValue = tupleValue->get(index);
					innerValueType = innerValue->getType();
                    switch(innerValueType->getType()) {
                        case NullType:
                        case IdentityType:
                        case TupleType:
                        case StandardOut:
                        case StandardIn:
                        case Lvalue:
                        case IntervalType: // TODO ACCOUNT FOR INTERVAL TYPE
						case StartVector:
                            printf("Tuple cannot contain this type\n");
                            exit(1);
                            break;
                        case BooleanType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(false));
							break;
                        case IntegerType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(0));
							break;
                        case RealType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(0.0f));
							break;
                        case CharacterType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value((char)0));
							break;
                    }
					innerValueType->release();
					innerValue->release();
					innerValue = nullptr;
					innerValueType = nullptr;
                    ++index;
                }
				tupleValue->release();
				if (innerValueType != nullptr) { innerValueType->release(); }
				if (innerValue != nullptr) { innerValue->release(); }
                break;
			case IntervalType:
				rType = new ValueType(IntervalType);
			    rvalue = new Value(rType, new Vector<Value>());
				intervalValue = (*var)->intervalValue();
                varSize = intervalValue->getCount();
                index = 0;
                while(index < varSize) {
                    innerValue = intervalValue->get(index);
					innerValueType = innerValue->getType();
                    switch(innerValueType->getType()) {
                        case NullType:
                        case IdentityType:
                        case TupleType:
                        case StandardOut:
                        case StandardIn:
                        case Lvalue:
                        case BooleanType:
                        case RealType:
                        case CharacterType:
                        case IntervalType:
						case StartVector:
                            printf("Interval cannot contain this type\n");
                            exit(1);
                            break;
                        case IntegerType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(0));
							break;
                    }
					innerValueType->release();
					innerValue->release();
					innerValue = nullptr;
					innerValueType = nullptr;
                    ++index;
                }
				intervalValue->release();
				if (innerValueType != nullptr) { innerValueType->release(); }
				if (innerValue != nullptr) { innerValue->release(); }
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
			case StartVector:
				printf("Cannot start Vector");
				exit(1);
				break;
		}
		valueType->release();
	}

	// Transform the rvalue to the default value when using `identity`
	if (rvalue->isIdentity()) {
		rvalue->release();
		rvalue = nullptr;
		ValueType* rType = nullptr;
		int varSize;
		int index;
		Value* innerValue = nullptr;
		Vector<Value>* tupleValue = nullptr;
		Vector<Value>* intervalValue = nullptr;
		ValueType* valueType = (*var)->getType();
		ValueType* innerValueType = nullptr;
		Vector<Value>* rvalueVector = nullptr;
		switch (valueType->getType()) {
			case NullType:
			case IdentityType:
				printf("Cannot determine type");
				exit(1);
				break;
			case BooleanType:	rvalue = new Value(true);		break;
			case IntegerType:	rvalue = new Value(1);			break;
			case RealType:		rvalue = new Value(1.0f);		break;
			case CharacterType:	rvalue = new Value((char)1);	break;
			case IntervalType:
				rType = new ValueType(IntervalType);
			    rvalue = new Value(rType, new Vector<Value>());
				intervalValue = (*var)->intervalValue();
                varSize = intervalValue->getCount();
                index = 0;
                while(index < varSize) {
                    innerValue = intervalValue->get(index);
					innerValueType = innerValue->getType();
                    switch(innerValueType->getType()) {
                        case NullType:
                        case IdentityType:
                        case TupleType:
                        case StandardOut:
                        case StandardIn:
                        case Lvalue:
                        case BooleanType:
                        case RealType:
                        case CharacterType:
                        case IntervalType:
						case StartVector:
                            printf("Interval cannot contain this type\n");
                            exit(1);
                            break;
                        case IntegerType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(1));
							break;
                    }
					innerValueType->release();
					innerValue->release();
					innerValue = nullptr;
					innerValueType = nullptr;
                    ++index;
                }
			case TupleType:
				rType = new ValueType(TupleType);
				rvalue = new Value(rType, new Vector<Value>());
				tupleValue = (*var)->tupleValue();
				varSize = tupleValue->getCount();
				index = 0;
				while(index < varSize) {
					innerValue = tupleValue->get(index);
					innerValueType = innerValue->getType();
					switch(innerValueType->getType()) {
						case NullType:
						case IdentityType:
						case TupleType:
						case StandardOut:
						case StandardIn:
						case IntervalType: // TODO ACCOUNT FOR INTERVAL TYPE
						case Lvalue:
						case StartVector:
							printf("Tuple cannot contain this type\n");
							exit(1);
							break;
						case BooleanType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(true));
							break;
						case IntegerType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(1));
							break;
						case RealType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value(1.0f));
							break;
						case CharacterType:
							rvalueVector = (Vector<Value>*)rvalue->value_ptr();
							rvalueVector->append(new Value((char)1));
							break;
					}
					innerValueType->release();
					innerValue->release();
					innerValue = nullptr;
					innerValueType = nullptr;
					++index;
				}
				tupleValue->release();
				if (innerValueType != nullptr) { innerValueType->release(); }
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
			case StartVector:
				printf("Cannot Start Vector");
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
