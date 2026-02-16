import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HabitCalendarApp {

    private static final String DATA_FILE_NAME = ".habit-tracker-data.ser";

    private final JFrame frame = new JFrame("Monthly Habit Tracker");
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JTextField habitInput = new JTextField("Exercise, Read, Meditate", 28);
    private final JTextArea analysisArea = new JTextArea(8, 50);
    private final JLabel statusLabel = new JLabel("Ready");

    private final HabitTableModel tableModel = new HabitTableModel();
    private final String dataFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + DATA_FILE_NAME;

    private Map<YearMonth, MonthHabitData> monthData = new HashMap<>();
    private YearMonth currentMonth = YearMonth.now();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HabitCalendarApp().createAndShowUI());
    }

    private void createAndShowUI() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        JButton prevButton = new JButton("<");
        JButton nextButton = new JButton(">");
        JButton setHabitsButton = new JButton("Apply Habits");
        JButton saveButton = new JButton("Save Data");
        JButton loadButton = new JButton("Load Data");

        JPanel monthPanel = new JPanel(new BorderLayout(10, 0));
        monthLabel.setFont(new Font("Arial", Font.BOLD, 18));
        monthPanel.add(prevButton, BorderLayout.WEST);
        monthPanel.add(monthLabel, BorderLayout.CENTER);
        monthPanel.add(nextButton, BorderLayout.EAST);

        JPanel habitsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        habitsPanel.add(new JLabel("Habits (comma separated):"));
        habitsPanel.add(habitInput);
        habitsPanel.add(setHabitsButton);
        habitsPanel.add(saveButton);
        habitsPanel.add(loadButton);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(monthPanel);
        topPanel.add(habitsPanel);

        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane tableScrollPane = new JScrollPane(table);

        analysisArea.setEditable(false);
        analysisArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        analysisArea.setBorder(BorderFactory.createTitledBorder("Monthly Analysis"));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(analysisArea), BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(tableScrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        prevButton.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            loadMonth();
        });

        nextButton.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            loadMonth();
        });

        setHabitsButton.addActionListener(e -> {
            MonthHabitData data = getOrCreateMonthData(currentMonth);
            List<String> parsedHabits = parseHabits(habitInput.getText());
            data.setHabits(parsedHabits);
            tableModel.refreshForMonth(currentMonth);
            updateAnalysis();
            setStatus("Habits updated for " + currentMonth + ".");
            saveDataToDisk();
        });

        saveButton.addActionListener(e -> saveDataToDisk());

        loadButton.addActionListener(e -> {
            loadDataFromDisk();
            loadMonth();
            setStatus("Data loaded from " + dataFilePath);
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveDataToDisk();
                frame.dispose();
            }
        });

        loadDataFromDisk();
        tableModel.refreshForMonth(currentMonth);
        loadMonth();

        frame.setSize(980, 680);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void loadMonth() {
        monthLabel.setText(currentMonth.getMonth() + " " + currentMonth.getYear());
        MonthHabitData data = getOrCreateMonthData(currentMonth);
        habitInput.setText(String.join(", ", data.habits));
        tableModel.refreshForMonth(currentMonth);
        updateAnalysis();
    }

    private void updateAnalysis() {
        MonthHabitData data = getOrCreateMonthData(currentMonth);
        int days = currentMonth.lengthOfMonth();
        int habitCount = data.habits.size();

        int totalPossible = days * habitCount;
        int completed = 0;
        int perfectDays = 0;
        int bestStreak = 0;
        int currentStreak = 0;
        int[] habitTotals = new int[habitCount];

        for (int day = 1; day <= days; day++) {
            LocalDate date = currentMonth.atDay(day);
            boolean[] values = data.getCompletionsForDate(date);
            boolean allDone = true;

            for (int i = 0; i < habitCount; i++) {
                if (values[i]) {
                    completed++;
                    habitTotals[i]++;
                } else {
                    allDone = false;
                }
            }

            if (allDone) {
                perfectDays++;
                currentStreak++;
                bestStreak = Math.max(bestStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        int overallPercent = totalPossible == 0 ? 0 : (completed * 100) / totalPossible;
        StringBuilder analysis = new StringBuilder();
        analysis.append("Month: ").append(currentMonth).append("\n");
        analysis.append("Overall completion: ").append(overallPercent).append("% (")
                .append(completed).append("/").append(totalPossible).append(")\n");
        analysis.append("Perfect days: ").append(perfectDays).append("/").append(days).append("\n");
        analysis.append("Best full-completion streak: ").append(bestStreak).append(" day(s)\n\n");
        analysis.append("Habit-wise performance:\n");

        for (int i = 0; i < habitCount; i++) {
            int habitPercent = days == 0 ? 0 : (habitTotals[i] * 100) / days;
            analysis.append("- ").append(data.habits.get(i)).append(": ")
                    .append(habitPercent).append("% (")
                    .append(habitTotals[i]).append("/").append(days).append(")\n");
        }

        analysisArea.setText(analysis.toString());
        analysisArea.setCaretPosition(0);
    }

    private MonthHabitData getOrCreateMonthData(YearMonth month) {
        return monthData.computeIfAbsent(month, ignored -> new MonthHabitData(parseHabits(habitInput.getText())));
    }

    private List<String> parseHabits(String text) {
        List<String> parsed = new ArrayList<>();
        Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(parsed::add);

        if (parsed.isEmpty()) {
            parsed.add("Habit 1");
            parsed.add("Habit 2");
            parsed.add("Habit 3");
        }

        return parsed;
    }

    private void saveDataToDisk() {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(dataFilePath))) {
            objectOutputStream.writeObject(monthData);
            setStatus("Saved successfully to " + dataFilePath);
        } catch (IOException ex) {
            setStatus("Save failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(frame, "Unable to save data.\n" + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDataFromDisk() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(dataFilePath))) {
            Object loaded = objectInputStream.readObject();
            if (loaded instanceof Map<?, ?> loadedMap) {
                monthData = (Map<YearMonth, MonthHabitData>) loadedMap;
                setStatus("Loaded existing data from " + dataFilePath);
            }
        } catch (IOException | ClassNotFoundException ex) {
            monthData = new HashMap<>();
            setStatus("No existing data found yet. Start tracking and save.");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    class HabitTableModel extends AbstractTableModel {
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM (EEE)");

        @Override
        public int getRowCount() {
            return currentMonth.lengthOfMonth();
        }

        @Override
        public int getColumnCount() {
            return getOrCreateMonthData(currentMonth).habits.size() + 1;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Date";
            }
            return getOrCreateMonthData(currentMonth).habits.get(column - 1);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? String.class : Boolean.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex > 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LocalDate date = currentMonth.atDay(rowIndex + 1);
            if (columnIndex == 0) {
                return dateFormatter.format(date);
            }

            MonthHabitData data = getOrCreateMonthData(currentMonth);
            return data.getCompletionsForDate(date)[columnIndex - 1];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return;
            }

            MonthHabitData data = getOrCreateMonthData(currentMonth);
            LocalDate date = currentMonth.atDay(rowIndex + 1);
            data.getCompletionsForDate(date)[columnIndex - 1] = (Boolean) aValue;
            updateAnalysis();
            saveDataToDisk();
        }

        public void refreshForMonth(YearMonth month) {
            currentMonth = month;
            fireTableStructureChanged();
        }
    }

    static class MonthHabitData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<String> habits;
        private final Map<LocalDate, boolean[]> completionByDate = new HashMap<>();

        MonthHabitData(List<String> habits) {
            this.habits = new ArrayList<>(habits);
        }

        void setHabits(List<String> newHabits) {
            int oldHabitCount = habits.size();
            habits = new ArrayList<>(newHabits);
            int newHabitCount = habits.size();

            for (Map.Entry<LocalDate, boolean[]> entry : completionByDate.entrySet()) {
                boolean[] resized = new boolean[newHabitCount];
                boolean[] old = entry.getValue();
                System.arraycopy(old, 0, resized, 0, Math.min(oldHabitCount, newHabitCount));
                entry.setValue(resized);
            }
        }

        boolean[] getCompletionsForDate(LocalDate date) {
            return completionByDate.computeIfAbsent(date, ignored -> new boolean[habits.size()]);
        }
    }
}
