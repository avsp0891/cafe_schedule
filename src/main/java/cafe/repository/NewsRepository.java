package cafe.repository;

import cafe.model.News;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
}
