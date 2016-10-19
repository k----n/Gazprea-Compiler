#pragma once

//template <class T>
//Vector<T>::Vector() {
//	this->firstNode = nullptr;
//	this->count = 0;
//}
//
//template <class T>
//Vector<T>::Vector(const Vector<T>& original) {
//	this->firstNode = nullptr;
//	this->count = 0;
//	for (int i = 0; i < original.count; ++i) {
//		this->append(new T(*original.get(i)));
//	}
//}
//
//template <class T>
//Vector<T>::~Vector() {
//	VectorNode* node = this->firstNode;
//	while (node != nullptr) {
//		VectorNode* nextNode = nullptr;
//		delete (T*)node->value;
//		delete node;
//		node = nextNode;
//	}
//}
//
//template <class T>
//void Vector<T>::append(T *element) {
//	VectorNode* newNode = new VectorNode;
//	newNode->next = nullptr;
//	newNode->value = new T(*element);
//	if (this->firstNode == nullptr) {
//		this->firstNode = newNode;
//	} else {
//		VectorNode* node = this->firstNode;
//		while (node->next != nullptr) {
//			node = node->next;
//		}
//		node->next = newNode;
//	}
//	this->count += 1;
//}
//
//template <class T>
//T* Vector<T>::get(int index) const {
//	if (index >= this->count) { return nullptr; }
//	VectorNode* node = this->firstNode;
//	for (int i = 0; i < index; ++i) {
//		node = node->next;
//	}
//	return (T*)node->value;
//}
//
//template <class T>
//int Vector<T>::getCount() {
//	return this->count;
//}
//
//template <class T>
//Vector<T>* Vector<T>::map(functional mapFunction) {
//	Vector<T>* newVector = new Vector<T>();
//	for (int i = 0; i < this->count; ++i) {
//		functionalVectorArgumentValue = this->get(i);
//		mapFunction();
//		CalculatorValue* item = stack->pop();
//		newVector->append(item);
//		delete item;
//	}
//	functionalVectorArgumentValue = nullptr;
//	return newVector;
//}
//
//template <class T>
//Vector<T>* Vector<T>::filter(functional filterFunction) {
//	Vector<T>* newVector = new Vector<T>();
//	for (int i = 0; i < this->count; ++i) {
//		functionalVectorArgumentValue = this->get(i);
//		filterFunction();
//		if (toBool()) {
//			newVector->append((CalculatorValue*)functionalVectorArgumentValue);
//		}
//	}
//	functionalVectorArgumentValue = nullptr;
//	return newVector;
//}
