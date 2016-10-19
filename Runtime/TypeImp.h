#pragma once

Type::Type(BuiltinType type) {
	this->type = type;
//	this->subtypes = new Vector<Type>;
}

Type::~Type() {
//	delete this->subtypes;
}

BuiltinType Type::getType() {
	return this->type;
}

//Vector<Type>* Type::getSubtypes() {
//	return this->subtypes;
//}
