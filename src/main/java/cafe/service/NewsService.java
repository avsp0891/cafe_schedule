package cafe.service;

import cafe.dto.NewsDto;
import cafe.exception.ResourceNotFoundException;
import cafe.model.News;
import cafe.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    public List<NewsDto> getAll() {
        return newsRepository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<NewsDto> getPublished() {
        return getAll();
    }

    public NewsDto getById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        return toDto(news);
    }

    @Transactional
    public NewsDto create(NewsDto dto) {
        News news = News.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .publishedAt(Instant.now())
                .build();
        news = newsRepository.save(news);
        return toDto(news);
    }

    @Transactional
    public NewsDto update(Long id, NewsDto dto) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        news.setTitle(dto.getTitle());
        news.setContent(dto.getContent());
        news = newsRepository.save(news);
        return toDto(news);
    }

    @Transactional
    public void delete(Long id) {
        try {
            newsRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("News not found with id: " + id);
        }
    }

    private NewsDto toDto(News news) {
        return NewsDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .publishedAt(news.getPublishedAt())
                .build();
    }
}
