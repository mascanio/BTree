# BTree

Implementaci�n de un �rbol B, siguiendo la especifiaci�n en el [Cormen et. AL] (https://mitpress.mit.edu/books/introduction-algorithms) con ligeras modificaciones:

1. Se implementa como un mapa <Clave, Valor>, en el que la Clave debe ser comparable con ella misma (extiende a comparable de Clave)
2. El m�todo de Inserci�n devuelve null al insertar un par (clave, valor) si no exist�a una entrada en el TAD con la clave que se inserta; o si la clave ya exist�a, se devuelve el valor ya existente, y posteriormente se cambia por el que se est� insertando.
3. El m�todo de B�squeda(clave) devuelve null si no existe la clave insertada; o el valor asociado a dicha clave si existe.
4. El m�todo de borrado(clave) devuelve null si la clave no exist�a, o el valor asociado a dicha clave si estaba contenida en el �rbol.
5. En esta implementaci�n no se permiten repeticiones, si bien si se permite insertar una clave existente para sobrescribir el valor asociado ya existente.
6. Se implementan, adem�s, una funci�n para devolver el inorden del �rbol (como arrayList), y dos funciones para obtener la clave m�s peque�a y la m�s grandes del �rbol.
7. Se permite adem�s definir una funci�n, tipo funci�n hash, para establecer una relaci�n valor->clave. Esta funci�n, definida dentro de una clase abstracta, se pasa opcionalmente como par�metro a la constructora.

La implementaci�n se realiz� para una pr�ctica de la asignatura [M�todos algor�tmicos en resoluci�n de problemas] (http://www.fdi.ucm.es/Pub/ImpresoFichaDocente.aspx?Id=713) 