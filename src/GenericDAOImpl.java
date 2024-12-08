import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GenericDAOImpl<T> implements GenericDAO<T> {

    private final Class<T> entityClass;
    private final String tableName;

    public GenericDAOImpl(Class<T> entityClass, String tableName) {
        this.entityClass = entityClass;
        this.tableName = tableName;
    }

    // 插入
    @Override
    public void insert(T entity) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = generateInsertSQL(entity);
            if (conn != null) {
                pstmt = conn.prepareStatement(sql);
                setParameters(pstmt, entity, false);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, pstmt, null);
        }
    }

    // 更新
    @Override
    public void update(T entity) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = generateUpdateSQL(entity);
            if (conn != null) {
                pstmt = conn.prepareStatement(sql);
                setParameters(pstmt, entity, true);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, pstmt, null);
        }
    }

    // 删除
    @Override
    public void delete(T entity) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            String sql = "DELETE FROM " + tableName + " WHERE id = ?";
            if (conn != null) {
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, getEntityId(entity));
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, pstmt, null);
        }
    }

    // 根据ID查询
    @Override
    public T selectById(int id) {
        List<T> list = selectByCondition("id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    // 查询所有
    @Override
    public List<T> selectAll() {
        return selectByCondition(null);
    }

    // 根据条件查询
    @Override
    public List<T> selectByCondition(String condition, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<T> list = new ArrayList<>();
        try {
            conn = getConnection();
            String sql = "SELECT * FROM " + tableName + (condition != null ? " WHERE " + condition : "");
            if (conn != null) {
                pstmt = conn.prepareStatement(sql);
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                rs = pstmt.executeQuery();
            }
            if (rs != null) {
                while (rs.next()) {
                    T instance = entityClass.getDeclaredConstructor().newInstance();
                    for (Field field : entityClass.getDeclaredFields()) {
                        field.setAccessible(true);
                        field.set(instance, rs.getObject(field.getName()));
                    }
                    list.add(instance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(conn, pstmt, rs);
        }
        return list;
    }

    // 辅助方法
    private void setParameters(PreparedStatement pstmt, T entity, boolean isUpdate) throws IllegalAccessException, SQLException {
        int index = 1;
        for (Field field : entityClass.getDeclaredFields()) {
            if (!isUpdate || !field.getName().equals("id")) {
                field.setAccessible(true);
                pstmt.setObject(index++, field.get(entity));
            }
        }
        if (isUpdate) {
            pstmt.setInt(index, getEntityId(entity));
        }
    }

    private String generateInsertSQL(T entity) throws IllegalAccessException {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(entity) != null) {
                columns.append(field.getName()).append(", ");
                values.append("?, ");
            }
        }
        String colString = columns.substring(0, columns.length() - 2);
        String valString = values.substring(0, values.length() - 2);
        return "INSERT INTO " + tableName + " (" + colString + ") VALUES (" + valString + ")";
    }

    private String generateUpdateSQL(T entity) throws IllegalAccessException {
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.getName().equals("id")) {
                sql.append(field.getName()).append(" = ?, ");
            }
        }
        sql.setLength(sql.length() - 2); // 移除最后一个逗号
        sql.append(" WHERE id = ?");
        return sql.toString();
    }

    private int getEntityId(T entity) throws IllegalAccessException {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getName().equals("id")) {
                field.setAccessible(true);
                return (Integer) field.get(entity);
            }
        }
        throw new RuntimeException("Entity does not have an id field");
    }

    // 数据库连接和关闭资源的方法
    private Connection getConnection() throws SQLException {
        // 这里应该返回一个数据库连接
        return null; // 这里需要实现获取数据库连接的逻辑
    }

    private void close(Connection conn, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
