import java.util.List;

public interface GenericDAO<T> {
    void insert(T entity);
    void update(T entity);
    void delete(T entity);
    T selectById(int id);
    List<T> selectAll();
    List<T> selectByCondition(String condition, Object... params);
}
