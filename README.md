# BTree

Implementación de un árbol B, siguiendo la especifiación en el [Cormen et. AL] (https://mitpress.mit.edu/books/introduction-algorithms) con ligeras modificaciones:

1. Se implementa como un mapa <Clave, Valor>, en el que la Clave debe ser comparable con ella misma (extiende a comparable de Clave)
2. El método de Inserción devuelve null al insertar un par (clave, valor) si no existía una entrada en el TAD con la clave que se inserta; o si la clave ya existía, se devuelve el valor ya existente, y posteriormente se cambia por el que se está insertando.
3. El método de Búsqueda(clave) devuelve null si no existe la clave insertada; o el valor asociado a dicha clave si existe.
4. El método de borrado(clave) devuelve null si la clave no existía, o el valor asociado a dicha clave si estaba contenida en el árbol.
5. En esta implementación no se permiten repeticiones, si bien si se permite insertar una clave existente para sobrescribir el valor asociado ya existente.
6. Se implementan, además, una función para devolver el inorden del árbol (como arrayList), y dos funciones para obtener la clave más pequeña y la más grandes del árbol.
7. Se permite además definir una función, tipo función hash, para establecer una relación valor->clave. Esta función, definida dentro de una clase abstracta, se pasa opcionalmente como parámetro a la constructora.

La implementación se realizó para una práctica de la asignatura [Métodos algorítmicos en resolución de problemas] (http://www.fdi.ucm.es/Pub/ImpresoFichaDocente.aspx?Id=713) 