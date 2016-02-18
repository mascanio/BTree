package pkg;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

/**
 * 
 * @author Miguel Ascanio Gómez
 * 
 *         Implementación de un árbol-B según Cormen et.Al. con ligeras
 *         modificaciones para parecerse a la especificaciín de java.map
 *
 * @param <K>
 *            tipo de las claves utilizadas para mapear los valores
 * @param <V>
 *            tipo de los valores mapeados
 */
public class BTree<K extends Comparable<K>, V> {

	private final int t;
	private final ValueToKey valueToKey;
	private Node raiz;
	private int size;

	// B-TREE CREATE
	public BTree(int grado) {
		this(grado, null);
	}

	public BTree(int grado, ValueToKey valueToKey) {
		if (grado < 2)
			throw new IllegalArgumentException(
					"El valor del grado debe ser mayor que 1");
		this.t = grado;
		this.raiz = new Node();
		this.size = 0;

		this.valueToKey = valueToKey;
	}

	/**
	 * Clase para la función valueToKey
	 */
	public abstract class ValueToKey {
		abstract K toKey(V value);
	}

	private final Comparator<K> comparator = new Comparator<K>() {

		@Override
		public int compare(K o1, K o2) {
			return o1.compareTo(o2);
		}
	};

	protected class MyEntry implements Entry<K, V>, Comparable<K> {

		private K key;
		private V value;

		public MyEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			this.value = value;
			return value;
		}

		/**
		 * Permite comparar pares clave-valor con claves
		 * 
		 * @param o
		 * @return this.key.compareTo(o)
		 */
		@Override
		public int compareTo(K o) {
			return comparator.compare(this.key, o);
		}

		public String toString() {
			return key.toString();
		}

	}

	/**
	 * Clase privada de los nodos del árbol. Cada nodo se compone de varias
	 * entradas (de la clase MyEntry) y de una serie de hijos (de la clase Node)
	 */
	protected class Node {
		protected Object[] children;
		protected Object[] entry;

		protected int numOfKeys;

		protected boolean leaf;

		protected Node() {
			this.children = new Object[2 * t];
			this.entry = new Object[2 * t - 1];

			this.numOfKeys = 0;
			this.leaf = true;
			// Disk-Write
		}

		protected Node(MyEntry entry) {
			this();
			this.entry[this.numOfKeys++] = entry;
		}

		@SuppressWarnings("unchecked")
		protected Node getHijo(int pos) {
			return (BTree<K, V>.Node) children[pos];
		}

		@SuppressWarnings("unchecked")
		protected MyEntry getEntry(int pos) {
			return (BTree<K, V>.MyEntry) entry[pos];
		}

		@SuppressWarnings("unchecked")
		@Override
		public String toString() {
			String r = "";
			for (int i = 0; i < numOfKeys; i++) {
				r = r.concat(((MyEntry) (this.entry[i])).toString()) + ";";
			}
			if (leaf)
				return r;
			r += "\n(";
			for (int i = 0; i < numOfKeys + 1; i++) {
				r = r.concat(((Node) (this.children[i])).toString()) + "   ";
			}
			r += ")\n";
			return r;
		}
	}

	/**
	 * Funcion de úsqueda de un valor mapeado con una clave key, a partir de un
	 * nodo node
	 * 
	 * O(log n), siendo n el número de elemetos en el nodo (y en sus hijos)
	 * 
	 * @param node
	 *            nodo sobre el que buscar
	 * @param key
	 *            elemento a buscar
	 * @return el valor V asociado a la clave key en el caso de que esté
	 *         contenida en el nodo node o en alguno de sus hijos, null si no
	 *         existe entrada asociada a la clave key
	 */
	private V buscar(Node node, K key) {
		// Buscar elem en los elementos del nodo
		int pos = Arrays.binarySearch(node.entry, 0, node.numOfKeys, key);
		// Si está el elemento
		if (pos >= 0)
			return node.getEntry(pos).value;
		// Si no es el elemento, buscar en el hijo adecuado (el hijo en posición
		// pos: si pos = 0, y el anterior if no se cumplió, entonces el elem
		// que estamos buscando es menor que el primer elemento en nodo, por
		// tanto elem, de existir, debería encontrarse en el hijo de nodo en las
		// posición 0, este es el más a la izquierda)

		// Convertir posición (deacuerdo a implementación de BinarySearch
		pos = -(pos + 1);
		if (!node.leaf) // Si no se llega a final del árbol
			// Disk Read
			return buscar(node.getHijo(pos), key);
		else
			return null;
	}

	/**
	 * Divide el hijo del nodo x en la posición i, el cuál está lleno. Para ello
	 * se subirá una clave del hijo en la pos i a x, y se usarán las claves
	 * restantes como dos hijos de x, en las posiciones i e i+1
	 * 
	 * @param x
	 *            nodo del que se va a divivir un hijo, no pude estar lleno
	 * @param i
	 *            posición del hijo de x que es un nodo lleno
	 */
	private void split(Node x, int i) {
		if (x.numOfKeys >= (2 * t - 1))
			throw new AssertionError(
					"Se intenta dividir un hijo del cual el padre está lleno");
		Node z = new Node();
		Node y = x.getHijo(i);
		// El nodo y tendrá los hijos y claves a la izquierda de de x[i], z
		// tendrá los de la derecha
		z.leaf = y.leaf;

		// Poner las claves a la derecha de i en z
		for (int j = 0; j < t - 1; j++)
			z.entry[j] = y.entry[j + t];

		// Mover los hijos si no es hoja
		if (!y.leaf)
			for (int j = 0; j < t; j++)
				z.children[j] = y.children[j + t];
		z.numOfKeys = t - 1;
		y.numOfKeys = t - 1;

		// Desplazar a la derecha las claves de x, hasta la pos i (ésta será
		// desplazada)
		for (int j = x.numOfKeys - 1; j >= i; j--)
			x.entry[j + 1] = x.entry[j];
		// Insertar el elemento
		x.entry[i] = y.entry[t - 1];

		// Desplazar a la derecha los hijos de x, hasta la pos i+1 (ésta será
		// desplazada)
		for (int j = x.numOfKeys; j >= i + 1; j--)
			x.children[j + 1] = x.children[j];
		// Insertar el nuevo nodo
		x.children[i + 1] = z;

		x.numOfKeys = x.numOfKeys + 1;
		// DiskWrite
	}

	/**
	 * Inserta en el árbol, desde la raíz, la entrada MyEntry
	 * 
	 * Si la raíz está llena, se divide y se cambia la raíz según proceda
	 * 
	 * @param newEntry
	 *            entrada a insertar
	 * @return Entrada almacenada previamente (tendrá la misma clave que
	 *         newEntry, pero el valor puede diferir), o null si no existía
	 *         entrada con la clave de newEntry
	 */
	private MyEntry insert(MyEntry newEntry) {
		// Si el árbol estaba vacío, poner una nueva raíz
		if (raiz == null) {
			raiz = new Node();
			size = 0;
		}
		Node r = raiz;
		if (r.numOfKeys == 2 * t - 1) {
			// La raíz está llena, la dividimos, lo que se catapulte hacia
			// arriba será la nueva raíz (el árbol crece en altura)
			Node s = new Node();
			raiz = s;
			s.leaf = false;
			s.numOfKeys = 0;
			s.children[0] = r;
			split(s, 0);
			return insertNonFull(s, newEntry);
		} else {
			return insertNonFull(r, newEntry);
		}
	}

	/**
	 * Inserta en el nodo x (o en sus hijos), el cual no esá lleno, la entrada
	 * newEntry
	 * 
	 * @param x
	 *            nodo sobre el que se va a insertar
	 * @param newEntry
	 *            entrada a insertar
	 * @return Entrada almacenada previamente (tendrá la misma clave que
	 *         newEntry, pero el valor puede diferir), o null si no existía
	 *         entrada con la clave de newEntry
	 */
	private MyEntry insertNonFull(Node x, MyEntry newEntry) {
		MyEntry r = null; // Valor a devolver
		// Modificación Cormen: control de repeticiones:
		// en el Cormen no se controla la inserción de claves repetidas en el
		// árbol. Según la definición de árbolB, no puede haber claves
		// repetidas, sin embargo, no creo que esto sea razón para no controlar
		// la inserción de una clave ya existente. Además, para cumplir la
		// especificación de Java.util.Map, debemos devolver el valor previo al
		// que estamos insertando.
		// Para ello primero compruebo si la clave ya existe con el mismo
		// procedimiento que en buscar
		int pos = Arrays.binarySearch(x.entry, 0, x.numOfKeys, newEntry.key);
		if (pos >= 0) {
			// La clave de insercion ya existía, devolvemos la entrada
			// para poder conocer el valor anterior (según se especifica en
			// java.util.Map), y actualizamos su valor
			r = x.getEntry(pos);
			// Sólo haría falta actualizar el elem, no la clave
			r.setValue(newEntry.getValue());
			// Disk write
		} else { // Si no está
			if (x.leaf) {
				pos = -(pos + 1);
				// Nodo hoja, elem se debe insertar aquí en la posición pos
				// Desplazar nodos a la derecha (incluyendo pos)
				for (int j = x.numOfKeys - 1; j >= pos; j--)
					x.entry[j + 1] = x.entry[j];
				// Insertar la entrada
				x.entry[pos] = newEntry;
				x.numOfKeys++;
				this.size++;
				r = null; // La clave no existía
				// Disk write
			} else {
				// No es nodo hoja
				// Descender por el hijo a la derecha de la clave donde debería
				// estar la nueva entrada
				pos = -(pos + 1);
				// Disk read

				// Si el hijo donde vamos a insertar está lleno, lo dividimos y
				// recalculamos la posición donde insertar
				if (x.getHijo(pos).numOfKeys == 2 * t - 1) {
					split(x, pos);
					if (newEntry.compareTo(x.getEntry(pos).key) > 0)
						pos++;
				}
				r = insertNonFull(x.getHijo(pos), newEntry);
			}
		}
		return r;
	}

	/**
	 * Método que elimina del nodo x la entrada con clave key
	 * 
	 * En caso de que la raíz quedara vacía, se cambiaría a su único hijo (el
	 * árbol decrece en altura)
	 * 
	 * @param x
	 *            nodo del que se elimina
	 * @param key
	 *            clave de la entrada a eliminar
	 * @return Valor que se elimina, null si no existía entrada con clave key
	 */
	private V remove(Node x, K key) {
		int pos = Arrays.binarySearch(x.entry, 0, x.numOfKeys, key);
		V oldValue = null;
		if (x.leaf) {
			if (pos >= 0) {
				// Si está en esta hoja
				oldValue = x.getEntry(pos).getValue();
				// Borrarlo
				for (int i = pos; i < x.numOfKeys - 1; i++) {
					x.entry[i] = x.entry[i + 1];
				}
				x.numOfKeys--;
				size--;
			} // Else no esta -> return null
		} else {
			if (pos >= 0) {
				// Si el hijo está en este nodo (en el array entry de x), lo
				// eliminamos con cuidado de mantener el invariante
				// x.numOfKeys > t-1.
				oldValue = removeExisting(x, pos);
			} else {
				// No esta en este nodo
				// Hay que seguir bajando por el árbol, asegurando que haya al
				// menos t entradas en cada nodo
				pos = -(pos + 1); // Hijo donde debería estar key
				oldValue = removeNoExisting(x, pos, key);
			}
		}
		// if oldValue != null Disk write
		if (x == raiz && x.numOfKeys == 0) {
			raiz = x.getHijo(0);
			// Disk write
		} else if (x != raiz && x.numOfKeys < t - 1)
			throw new AssertionError("x.numOfKeys < t-1");
		return oldValue;
	}

	/**
	 * Método para borrar del hijo en posición pos del nodo x, la entrada con
	 * clave k, sabiendo que dicha entrada no está en las entradas de x
	 * 
	 * @param x
	 *            nodo que no contiene, en su array de entradas, la entrada con
	 *            clave key que queremos borrar
	 * @param pos
	 *            posición, en el array de hijos, donde debería estar key
	 * @param key
	 *            clave de la enrtada a borrar
	 * @return Valor que se elimina, null si no existía entrada con clave key
	 */
	private V removeNoExisting(Node x, int pos, K key) {
		V oldValue;
		Node y = x.getHijo(pos);
		if (y.numOfKeys > t - 1) {
			// Podemos quitar un hijo a y (no ahora, pero la llamada
			// recursiva puede hacerlo) sin romper el invariante
			oldValue = remove(y, key);
		} else {
			// No podemos seguir hacia abajo, ya que si quitáramos algo
			// al nodo y romperíamos el invariante.
			int numKeyLeft = -1, numKeyRight = -1;
			if (pos > 0)
				numKeyLeft = x.getHijo(pos - 1).numOfKeys;
			if (pos < x.numOfKeys)
				numKeyRight = x.getHijo(pos + 1).numOfKeys;
			// Quitarlo al que más tenga
			boolean maxIsLeft = numKeyLeft > numKeyRight;
			int max = maxIsLeft ? numKeyLeft : numKeyRight;
			if (max > t - 1) {
				if (maxIsLeft) {
					Node z = x.getHijo(pos - 1);
					// Bajo al nodo y la entrada de x en pos-1
					// Necesariamente x[pos-1] es menor que cualquier
					// entrada en y
					// La sitúo a la izquierda de y
					for (int i = y.numOfKeys - 1; i >= 0; i--) {
						y.entry[i + 1] = y.entry[i];
					}
					y.entry[0] = x.getEntry(pos - 1);
					// Subo a x la entrada más a la derecha del nodo z
					x.entry[pos - 1] = z.entry[z.numOfKeys - 1];
					// Si z no es hoja (y por tanto y tampoco) pongo el
					// hijo de z que acabo de subir a x colgando del
					// hijo que he bajado a y
					// Nótese que este hijo estaba a la izquierda de la
					// entrada x[pos-1], y ahora lo sigue estando
					if (!z.leaf) {
						for (int i = y.numOfKeys; i >= 0; i--) {
							y.children[i + 1] = y.children[i];
						}
						y.children[0] = z.children[z.numOfKeys];
					}
					z.numOfKeys--;
					y.numOfKeys++;
					// No he tocado el número de elementos de x
					//
					// Ahora y (el nodo que tiene k) tiene al menos t
					// entradas (y por lo tanto podría perder una sin
					// romper el invariante)
					//
					// Ahora z (el adyacente a y con más entradas) tiene
					// una entrada menos (pero no menos que t-1)
					if (y.numOfKeys < t)
						throw new AssertionError("y.numOfKeys < t");
					if (z.numOfKeys < t - 1)
						throw new AssertionError("z.numOfKeys < t-1");

					// Sigo borrando por el nodo y (que es donde debería
					// estar k), que ya tiene al menos t elementos
					oldValue = remove(y, key);

				} else { // MaxIsRight
					Node z = x.getHijo(pos + 1);
					// Bajo al nodo y la entrada de x en pos
					// Necesariamente x[pos] es mayor que cualquier
					// entrada en y
					// La sitúo a la derecha de y
					y.entry[y.numOfKeys] = x.getEntry(pos);
					// Subo a x la entrada más a la izquierda del nodo z
					x.entry[pos] = z.entry[0];
					for (int i = 0; i < z.numOfKeys - 1; i++) {
						z.entry[i] = z.entry[i + 1];
					}
					// Si z no es hoja (y por tanto y tampoco) pongo el
					// hijo de z que acabo de subir a x colgando del
					// hijo que he bajado a y
					// Nótese que este hijo estaba a la derecha de la
					// entrada x[pos], y ahora lo sigue estando
					if (!z.leaf) {
						y.children[y.numOfKeys + 1] = z.children[0];
						for (int i = 0; i < z.numOfKeys; i++) {
							z.children[i] = z.children[i + 1];
						}
					}
					z.numOfKeys--;
					y.numOfKeys++;
					// No he tocado el número de elementos de x
					//
					// Ahora y (el nodo que tiene k) tiene al menos t
					// entradas (y por lo tanto podría perder una sin
					// romper el invariante)
					//
					// Ahora z (el adyacente a y con más entradas) tiene
					// una entrada menos (pero no menos que t-1)
					if (y.numOfKeys < t)
						throw new AssertionError("y.numOfKeys < t");
					if (z.numOfKeys < t - 1)
						throw new AssertionError("z.numOfKeys < t-1");

					// Sigo borrando por el nodo y (que es donde debería
					// estar k), que ya tiene al menos t elementos
					oldValue = remove(y, key);
				}
			} else {
				// Los dos hermanos tienen t-1 keys y por tanto no
				// podemos quitarle nada a ninguno
				// Si pos > 0, arbitrariamente fusionamos el hijo donde
				// está k con el que está inmediatamente a la izquierda
				// Si pos == 0, fusionamos el hijo donde está k con el
				// que está inmediatamente a la derecha
				if (pos > 0) {
					merge(x, pos - 1, x.getHijo(pos - 1), y);
					oldValue = remove(x.getHijo(pos - 1), key);
				} else {
					merge(x, pos, y, x.getHijo(pos + 1));
					oldValue = remove(y, key);
				}
			}
		}
		return oldValue;
	}

	/**
	 * @param x
	 *            nodo que va a perder una entrada (de su vector entry)
	 * @param pos
	 *            pos en la que se encuentra la entrada a eliminar
	 * @return valor asociado a x.entry[pos], previo al borrado
	 */
	private V removeExisting(Node x, int pos) {
		// Si está en este nodo, lo vamos a eliminar sustituyéndolo por
		// una clave del hijo correspondiente (nos vale la clave más
		// grande del hijo que va a la izquierda de k, o la clave más
		// pequeña del hijo que va a la derecha de k)
		V oldValue;
		Node y = x.getHijo(pos);
		if (y.numOfKeys >= t) {
			// En este caso y puede perder una clave sin romper el
			// invariante
			// La clave que sube es la última de y
			MyEntry movingEntry = lastKey(y);
			remove(y, movingEntry.getKey());
			oldValue = x.getEntry(pos).getValue();
			x.entry[pos] = movingEntry;
		} else {
			// Si a y le quitaramos una clave romeríamos el invariante
			// Intentamos subir la clave más pequeña del hijo a la
			// derecha de k
			Node z = x.getHijo(pos + 1);
			if (z.numOfKeys >= t) {
				// Podemos quitar un hijo a z sin romper el invariante
				// La clave que sube es la primera de z
				MyEntry movingEntry = firstKey(z);
				remove(z, movingEntry.getKey());
				oldValue = x.getEntry(pos).getValue();
				x.entry[pos] = movingEntry;
			} else {
				if (y.numOfKeys != t - 1 || z.numOfKeys != t - 1)
					throw new AssertionError(
							"y.numOfKeys != t - 1 || z.numOfKeys != t - 1");
				// Tampoco podemos quitar un hijo a z Sin embargo por el
				// invariante sabemos que numKeys(n) >= t-1 para
				// cualquier nodo n, con las condiciones de los if
				// sabemos que numOfkeys(y) && numOfKeys(z) < t por lo
				// tanto y,z tienen exactamente t-1 claves, por lo
				// tanto los podemos unir y,k,z en un unico nodo (lo
				// metemos en y), teniendo 2t-1 claves (cumpliendo el
				// invariante)
				// Acto seguido eliminamos k de este "Nuevo nodo"
				oldValue = x.getEntry(pos).getValue();
				// Merge
				MyEntry deletingEntry = merge(x, pos, y, z);
				// Borrarla del "nuevo" nodo y
				remove(y, deletingEntry.getKey());
			}
		}
		return oldValue;
	}

	/**
	 * Fusion de nodos
	 * 
	 * @param x
	 *            Nodo que va a ver sus hijos fusionados
	 * @param pos
	 *            Entrada que va a perder para la fusión
	 * @param y
	 *            Nodo que va a acabar siendo la fusión de y con z (el nodo que
	 *            prevalece)
	 * @param z
	 *            Nodo que se acaba fusionando con y (el nodo que desaparece)
	 * @return La entrada que x pierde para llevar a cabo la fusión
	 * @throws AssertionError
	 */
	private MyEntry merge(Node x, int pos, Node y, Node z) {
		// Poner k en y
		y.entry[y.numOfKeys++] = x.getEntry(pos);
		MyEntry deletingEntry = x.getEntry(pos);
		// Poner claves de z en y
		for (int i = 0; i < z.numOfKeys; i++) {
			y.entry[y.numOfKeys + i] = z.entry[i];
		}
		// Poner los hijos de z en y
		if (!z.leaf) {
			for (int i = 0; i < z.numOfKeys + 1; i++) {
				y.children[y.numOfKeys + i] = z.children[i];
			}
		}
		y.numOfKeys = 2 * t - 1;
		// Quitar k y z de x
		for (int i = pos; i < x.numOfKeys - 1; i++) {
			x.entry[i] = x.entry[i + 1];
			x.children[i + 1] = x.children[i + 2];
		}
		x.numOfKeys--;
		if (x != raiz && x.numOfKeys < t - 1)
			throw new AssertionError("x != raiz && x.numOfKeys < t-1");
		return deletingEntry;
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return raiz == null || size == 0;
	}

	@SuppressWarnings("unchecked")
	/**
	 * Método para determinar si el árbol contiene mapeo para la clave key
	 * 
	 * Se realiza en O(logn)
	 * 
	 * @param key
	 * @return true si contiene una enrtada para la clave key
	 */
	public boolean containsKey(Object key) {
		if (isEmpty() || key == null)
			return false;
		return buscar(raiz, (K) key) != null;
	}

	@SuppressWarnings("unchecked")
	/**
	 * Función que busca si se contiene el valor value. 
	 * Si se ha definido ValueToKey, se usa para buscar por clave en O(logn), si no, se realiza una búsqueda lineal
	 * 
	 * @param value valor a buscar
	 * @return true si se encuentra
	 */
	public boolean containsValue(Object value) {
		if (isEmpty() || value == null)
			return false;
		if (valueToKey == null) {
			return containsValue(raiz, (V) value);
		} else
			return containsKey(valueToKey.toKey((V) value));
	}

	private boolean containsValue(Node x, V value) {
		for (int i = 0; i < x.numOfKeys; i++) {
			if (value.equals(x.getEntry(i).value))
				return true;
			if (!x.leaf && containsValue(x.getHijo(i), value))
				return true;
		}

		return (!x.leaf && containsValue(x.getHijo(x.numOfKeys), value));
	}

	@SuppressWarnings("unchecked")
	/**
	 * Metodo que devuelve el valor asociado a la clave key
	 * 
	 * Se realiza en O(logn)
	 *
	 * @param key
	 * @return valor asociado a key, null si no existe mapeo para esta clave
	 */
	public V get(Object key) {
		return isEmpty() ? null : buscar(raiz, (K) key);
	}

	/**
	 * Método que inserta el valor value con clave key
	 * 
	 * Se realiza en O(logn)
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public V put(K key, V value) {
		if (key == null)
			throw new NullPointerException("La clave no puede ser null");
		MyEntry r = insert(new MyEntry(key, value));
		return r == null ? null : r.value;
	}

	/**
	 * Método que elimina del árbol la entrada con clave key, si existiera
	 * 
	 * Se realiza en O(logn)
	 * 
	 * @param key
	 * @return el valor asociado a key previo al borrado, null si no existe
	 *         mapeo para key
	 */
	@SuppressWarnings("unchecked")
	public V remove(Object key) {
		return isEmpty() ? null : remove(raiz, (K) key);
	}

	/**
	 * Inserta todos los elementos del mapa m en el árbol
	 * 
	 * @param m
	 */
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Iterator<?> iterator = m.entrySet().iterator(); iterator.hasNext();) {
			@SuppressWarnings("unchecked")
			Entry<? extends K, ? extends V> entry = (java.util.Map.Entry<? extends K, ? extends V>) iterator
					.next();
			insert(new MyEntry(entry.getKey(), entry.getValue()));
		}
	}

	public void clear() {
		this.raiz = new Node();
		this.size = 0;
	}

	public String toString() {
		return raiz.toString();
	}

	/**
	 * 
	 * @return comparador utilizado para comparar claves
	 */
	public Comparator<? super K> comparator() {
		return comparator;
	}

	/**
	 * 
	 * @return la primera entrada del arbol (aquella con la clave más pequeña)
	 */
	public Entry<K, V> firstEntry() {
		return isEmpty() ? null : firstKey(raiz);
	}

	private MyEntry firstKey(Node x) {
		if (x.leaf)
			return x.getEntry(0);
		else
			return firstKey(x.getHijo(0));
	}

	/**
	 * 
	 * @return la última entrada del arbol (aquella con la clave más grande)
	 */
	public Entry<K, V> lastEntry() {
		return isEmpty() ? null : lastKey(raiz);
	}

	private MyEntry lastKey(Node x) {
		if (x.leaf)
			return x.getEntry(x.numOfKeys - 1);
		else
			return lastKey(x.getHijo(x.numOfKeys));
	}

	private ArrayList<K> inOrder(Node x) {
		ArrayList<K> r = new ArrayList<K>();
		if (!x.leaf)
			r.addAll(inOrder(x.getHijo(0)));
		for (int i = 0; i < x.numOfKeys; i++) {
			r.add(x.getEntry(i).getKey());
			if (!x.leaf)
				r.addAll(inOrder(x.getHijo(i + 1)));
		}
		return r;
	}

	/**
	 * 
	 * @return ArrayList con el inOrden del árbol (claves en orden creciente).
	 *         Devuelve un array vacío si el árbol es vacío (no devuelve null)
	 */
	public ArrayList<K> inOrderKey() {
		if (isEmpty())
			return new ArrayList<K>();
		return inOrder(raiz);
	}

	public static <T extends Comparable<? super T>> boolean isSorted(
			Iterable<T> iterable) {
		Iterator<T> iter = iterable.iterator();
		if (!iter.hasNext()) {
			return true;
		}
		T t = iter.next();
		while (iter.hasNext()) {
			T t2 = iter.next();
			if (t.compareTo(t2) > 0) {
				return false;
			}
			t = t2;
		}
		return true;
	}

	public static void main(String[] args) {
		casosPrueba();
		valida();
		try {
			bench();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void casosPrueba() {
		BTree<Integer, Integer> b = new BTree<>(2);
		for (int i = 1; i < 14; i++) {
			b.put(i, i);
			System.out.println("======================\n\n" + b);
		}
		b.remove(8);
		System.out.println("======================\n\n" + b);
		b.remove(4);
		System.out.println("======================\n\n" + b);
		b.remove(3);
		System.out.println("======================\n\n" + b);
		b.remove(2);
		System.out.println("======================\n\n" + b);
		b.remove(9);
		System.out.println("======================\n\n" + b);
		b.remove(1);
		System.out.println("======================\n\n" + b);
	}

	private static final int INI = 100000;
	private static final int MAX = 6000000;
	private static final int STEP = 50000;
	private static final int TEST = 3;

	private static void bench() throws IOException {
		FileWriter fw = new FileWriter("bench.txt");
		PrintWriter pw = new PrintWriter(fw);

		for (int numClaves = INI; numClaves <= MAX; numClaves += STEP) {
			System.out.println("Iteración numClaves = " + numClaves);
			pw.print("" + numClaves + " ");
			double timeInsert = 0;
			double timeSearch = 0;
			double timeDel = 0;

			for (int i = 0; i < TEST; i++) {
				System.out.println("Iteración i = " + i);
				BTree<Integer, Integer> b = new BTree<Integer, Integer>(10);
				ArrayList<Integer> arrayPrueba = new ArrayList<Integer>(
						numClaves);
				java.util.Random rd = new Random();

				for (int j = 0; j < numClaves; j++) {
					Integer n = rd.nextInt(numClaves * 100);
					arrayPrueba.add(n);
				}
				long start = System.currentTimeMillis();
				for (Iterator<Integer> iterator = arrayPrueba.iterator(); iterator
						.hasNext();) {
					Integer n = iterator.next();
					b.put(n, n);
				}
				long end = System.currentTimeMillis();
				timeInsert += (end - start);

				ArrayList<Integer> arrayPruebaBorra = new ArrayList<Integer>(
						arrayPrueba);
				Collections.shuffle(arrayPruebaBorra);

				start = System.currentTimeMillis();
				for (Iterator<Integer> iterator = arrayPruebaBorra.iterator(); iterator
						.hasNext();) {
					b.containsKey(iterator.next());
				}
				end = System.currentTimeMillis();
				timeSearch += (end - start);

				start = System.currentTimeMillis();
				for (Iterator<Integer> iterator = arrayPruebaBorra.iterator(); iterator
						.hasNext();) {
					b.remove(iterator.next());
				}
				end = System.currentTimeMillis();
				timeDel += (end - start);
			}

			pw.print("" + ((double) timeInsert * (1 / (double) TEST)) + " ");
			pw.print("" + ((double) timeSearch * (1 / (double) TEST)) + " ");
			pw.println("" + ((double) timeDel * (1 / (double) TEST)) + " ");
		}
		pw.close();
	}

	private static int LIM = 60000;

	public static void valida() {
		BTree<Integer, Integer> b = new BTree<Integer, Integer>(50);
		ArrayList<Integer> arrayPrueba = new ArrayList<Integer>(LIM);
		java.util.Random rd = new Random();

		System.out.println("Insertando números...");
		for (int i = 0; i < LIM; i++) {
			Integer n = rd.nextInt(LIM * 1000);
			b.put(n, n);
			if (!isSorted(b.inOrderKey()))
				throw new Error(
						"Error, no está ordenado durante la insercción!");

			arrayPrueba.add(n);
		}
		System.out.println("Números insertados");
		if (isSorted(b.inOrderKey()))
			System.out.println("Ordenado");
		else
			throw new Error("Error, no está ordenado!");

		for (Iterator<Integer> iterator = arrayPrueba.iterator(); iterator
				.hasNext();) {
			int n = iterator.next();
			Integer e = b.get(n);
			if (e == null)
				throw new Error("Error, mapeo incorrecto!");
			else if (e != n)
				throw new Error("Error, elemento no contenido!");

		}
		System.out
				.println("Se han insertado todso los elementos correctamente");

		ArrayList<Integer> arrayPruebaBorra = new ArrayList<Integer>(
				arrayPrueba);
		Collections.shuffle(arrayPruebaBorra);

		System.out.println("Borrando...");
		int i = 0;
		for (Iterator<Integer> iterator = arrayPruebaBorra.iterator(); iterator
				.hasNext();) {
			Integer n = iterator.next();
			if (b.containsKey(n)) {
				Integer removed = b.remove(n);
				if (b.containsKey(n)) {
					throw new Error("Error, no se ha borrado" + n);
				}
				if (!isSorted(b.inOrderKey())) {
					throw new Error("Error, borrar desordena" + i);
				}
				if (removed != null && !removed.equals(n)) {
					throw new Error("Error, se ha borrado lo que no se quería "
							+ i + "N = " + n + " REMOVED = " + removed);
				}
			}
			i++;
		}
		System.out.println("Borrado correcto");
		System.out.println("Probando PutAll");
		TreeMap<Integer, Integer> pruebaMap = new TreeMap<Integer, Integer>();
		for (Iterator<Integer> iterator = arrayPrueba.iterator(); iterator
				.hasNext();) {
			Integer n = iterator.next();
			pruebaMap.put(n, n);
		}
		b.putAll(pruebaMap);
		if (isSorted(b.inOrderKey()))
			System.out.println("Ordenado");
		else
			throw new Error("Error, no está ordenado!");
		i = 0;
		for (Iterator<Integer> iterator = arrayPruebaBorra.iterator(); iterator
				.hasNext();) {
			Integer n = iterator.next();
			if (!b.containsKey(n)) {
				throw new Error("PutAll incorrecto, no contiene" + n);
			}
		}
		System.out.println("PutAll correcto");
	}
}
