package com.renue;

import javax.imageio.IIOException;
import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        checkArgs(args); // Проверяем, правильно ли пользователь ввел аргумент
        int col = Integer.parseInt(args[0]) - 1; // Получаем номер колонки для поиска
        Integer[] digitCols = new Integer[]{0, 6, 7, 8, 9}; // Колонки, данные в которых - цифры
        List digitColsList = new ArrayList<>(Arrays.asList(digitCols));
        Tr root = readFromFile(col); // Создаем дерево из данных в указанной колонке

        /* Считываем строку с консоли */
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        System.out.println("Введите строку для поиска:");
        try {
            while ((userInput = reader.readLine()) != null) {
                if(userInput.equals("!quit")) {
                    System.exit(0);
                } else {
                    userInput = userInput.toLowerCase(); // Переводим строку в нижний регистр

                    ArrayList<String> been = new ArrayList<>(); // ArrayList, хранящий посещённые узлы дерево
                    ArrayList<Res> result = new ArrayList<>(); // ArrayList, хранящий результаты поиска

                    long time = System.currentTimeMillis(); // Замеряем время начала поиска
                    Tr foundNode = search(userInput, root); // Находим узел, значение которого ближе всего к искомому
                    result = display(foundNode, userInput, been, result); // Находим нужные узлы в алфавитном порядке
                    if (digitColsList.contains(col)) // Если поиск ведется по колонке с числами,
                        Collections.sort(result);    // сортируем результаты поиска в числовом порядке
                    long time2 = System.currentTimeMillis()-time; // Замеряем время окончания поиска

                    /* Выводим результаты поиска */
                    show(result, col);
                    System.out.println("Количество найденных строк: " + result.size() + "; Время, затраченное на поиск: " + time2 + " мс");

                    been.clear(); // Очищаем список посещенных узлов для нового поиска

                    System.out.println("Введите строку для поиска:");
                }
            };
        } catch (IIOException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Выводит результаты поиска по строке
     * @param result <code>ArrayList</code> с результатами поиска
     * @param col номер колонки, в которой вёлся поиск
     * @throws IOException
     */
    public static void show(ArrayList<Res> result, int col) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File("airports.csv"), "r");
        String line;

        for (Res res : result) {
            raf.seek(res.address); // Переходим в нужное место в файле
            line = raf.readLine();
            System.out.println(line.split(",")[col] + " [" + line + "]"); // Выводим результат
        }
    }

    /**
     * Проверяет корректность указанного пользователем номера столбца
     * @param args список аргументов
     */
    public static void checkArgs(String[] args){
        if (args.length != 1) {
            System.out.println("Требуется указать один аргумент - номер столбца");
            System.exit(1);
        }
        int col = Integer.parseInt(args[0]);
        if (col < 1 || col > 14){
            System.out.println("В файле нет столбца с таким номером");
            System.exit(1);
        }
    }

    /**
     * Добавляет узел в дерево поиска
     * @param str <code>String</code> со значением добавляемого узла
     * @param tree корень дерева, в которое будет добавлен узел
     * @param num адрес строки в файле, в которой содержится добавляемое значение
     */
    public static void add(String str, Tr tree, Long num) {
        if (str.compareTo(tree.val) < 0) { //если новая строка меньше, чем в узле
            if (tree.left != null) //если слева есть дерево
                add(str, tree.left, num); //идем в него
            else { //если нет дерева
                tree.left = new Tr(str, tree); //добавляем
                tree.left.num.add(num);
            }
        } else if (str.compareTo(tree.val) == 0) {
                tree.num.add(num);
        }
        else { //если новая строка больше, чем в узле
            if (tree.right != null) //если справа есть дерево
                add(str, tree.right, num); //идем в него
            else { //если нет дерева
                tree.right = new Tr(str, tree); //добавляем
                tree.right.num.add(num);
            }
        }
    }

    /**
     * Считывает данные из файла и создает дерево из указанной колонки
     * @param col номер колонки, по которой будет вестись поиск
     * @return получившееся дерево
     * @throws IOException
     */
    public static Tr readFromFile(int col) throws IOException {
        System.out.println("Создание дерева поиска...");

        RandomAccessFile raf = new RandomAccessFile(new File("airports.csv"), "r");
        String line;
        Long addr; // Адрес строки в файле
        Tr root = new Tr(); // Создаем пустое дерево
        if (((addr = raf.getFilePointer()) != null) && (line = raf.readLine()) != null) { // Считываем первую строку
            String[] values = line.split(",");

            if (values[col].charAt(0) == '"') // Если значение - строка
                root.val = values[col].substring(1, values[col].length()-1).toLowerCase(); // Создаем корень со строкой без кавычек
            else // Если значение - число
                root.val = values[col]; // Создаем корень с числом
            root.num.add(addr); // Добавляем адрес строки в корень
        }

        /* Считываем остальные значение колонки и добавляем их в дерево */
        while (((addr = raf.getFilePointer()) != null) && (line = raf.readLine()) != null) {
            String[] values = line.split(",");

            if (values[col].charAt(0) == '"')
                add(values[col].substring(1, values[col].length()-1).toLowerCase(), root, addr);
            else
                add(values[col], root, addr);
        }
        return root;
    }

    /**
     * Находит узел дерева, наиболее близкий к поисковому запросу
     * @param str строка поиска
     * @param tree дерево, в котором ведется поиск
     * @return поддерево - найденный узел
     */
    public static Tr search(String str, Tr tree) {
        if (tree.val.compareTo(str) > 0) { // Если значение текущего узла > искомого значения
            if (tree.left != null) { // Если можно перейти влево
                Tr res = search(str, tree.left); // Идем влево
                return res;
            } else {
                return tree;
            }
        } else if (tree.val.compareTo(str) < 0) { // Если значение текущего узла < искомого значения
            if (tree.right != null) { // Если можно перейти вправо
                Tr res = search(str, tree.right); // Идем вправо
                return res;
            } else {
                Tr res = tree.parent;
                return res;
            }
        } else {
            Tr res = tree;
            return res;
        }
    }

    /**
     * Обходит дерево с указанного узла и возвращает узлы, подходящие по критерию поиска в алфавитном порядке
     * @param node начальный узел
     * @param str строка для поиска
     * @param been список посещенных узлов
     * @param res список результатов поиска
     * @return список результатов поиска
     */
    public static ArrayList<Res> display(Tr node, String str, ArrayList been, ArrayList res) {
        if (node.val.startsWith(str)) {
            if (node.left != null && !been.contains(node.left.val)) {
                display(node.left, str, been, res);
            } else {
                if (node.right != null && !been.contains(node.right.val)) {
                    if (been.contains(node.val)) {
                        display(node.right, str, been, res);
                    } else {
                        if (node.val.startsWith(str))
                            for (Long num : node.num) {
                                res.add(new Res(num, node.val));
                            }
                        been.add(node.val);
                        display(node.right, str, been, res);
                    }
                } else {
                    if (been.contains(node.val)) {
                        if (node.parent == null) {
                            return res;
                        } else {
                            display(node.parent, str, been, res);
                        }
                    } else {
                        if (node.val.startsWith(str))
                            for (Long num : node.num) {
                                res.add(new Res(num, node.val));
                            }
                        been.add(node.val);
                        display(node.parent, str, been, res);
                    }
                }
            }
        } else {
            if (node.left != null && !been.contains(node.left.val)) {
                display(node.left, str, been, res);
            } else {
                if (node.parent == null) {
                    if (node.val.startsWith(str))
                        for (Long num : node.num) {
                            res.add(new Res(num, node.val));
                        }
                    been.add(node.val);
                    return res;
                } else {
                    been.add(node.val);
                    display(node.parent, str, been, res);
                }
            }
        }
        return res;
    }

    static class Tr {
        String val;
        Tr left;
        Tr right;
        Tr parent;
        ArrayList<Long> num;

        public Tr(String val, Tr left, Tr right, Tr parent, ArrayList<Long> num) {
            this.val = val;
            this.left = left;
            this.right = right;
            this.parent = parent;
            this.num = num;
        }

        public Tr(String val, Tr parent) {
            this.val = val;
            this.left = null;
            this.right = null;
            this.parent = parent;
            this.num = new ArrayList<Long>();
        }

        public Tr() {
            this.val = null;
            this.left = null;
            this.right = null;
            this.parent = null;
            this.num = new ArrayList<Long>();
        }
    }

    static class Res implements Comparable<Res> {
        Long address;
        String val;

        public Res(Long address, String val) {
            this.address = address;
            this.val = val;
        }

        @Override
        public int compareTo(Res o) {
            double val1 = Double.parseDouble(this.val);
            double val2 = Double.parseDouble(o.val);
            double res = val1 - val2;

            if (res > 0)
                return 1;
            else if (res < 0)
                return -1;
            else
                return 0;
        }

    }
}
