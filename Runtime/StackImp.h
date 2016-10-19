#pragma once

template <class T>
Stack<T>::Stack() {
	this->topNode = nullptr;
}

template <class T>
Stack<T>::~Stack() {
	StackNode* node = this->topNode;
	while (node != nullptr) {
		StackNode* nextNode = node->next;
		delete (T*)node->value;
		delete node;
		node = nextNode;
	}
}

template <class T>
void Stack<T>::push(T* element) {
	StackNode* newNode = new StackNode;
	newNode->next = this->topNode;
	newNode->value = new T(*element);
	this->topNode = newNode;
}

template <class T>
T* Stack<T>::pop() {
	if (this->topNode == nullptr) return nullptr;
	StackNode* node = this->topNode;
	this->topNode = this->topNode->next;
	T* value = (T*)node->value;
	delete node;
	return value;
}
