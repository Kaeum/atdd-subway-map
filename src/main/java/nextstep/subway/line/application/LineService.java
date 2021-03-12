package nextstep.subway.line.application;

import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.line.exception.*;
import nextstep.subway.station.exception.NoStationFoundException;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {
    private LineRepository lineRepository;
    private StationRepository stationRepository;

    public LineService(final LineRepository lineRepository, final StationRepository stationRepository) {
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional
    public LineResponse saveLine(final LineRequest lineRequest) {
        Station upStation = stationRepository.findById(lineRequest.getUpStationId()).orElseThrow(NoStationFoundException::new);
        Station downStation = stationRepository.findById(lineRequest.getDownStationId()).orElseThrow(NoStationFoundException::new);

        Line line = Line.of(lineRequest.getName(), lineRequest.getColor());

        line.addSection(upStation, downStation, lineRequest.getDistance());

        lineRepository.save(line);

        return LineResponse.of(line);
    }

    @Transactional(readOnly = true)
    public List<LineResponse> findAllLines() {
        List<Line> lines = lineRepository.findAll();

        return lines.stream()
                .map(line -> LineResponse.of(line))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LineResponse getLine(final Long id) {
        return lineRepository.findById(id)
                .map(l -> LineResponse.of(l))
                .orElseThrow(() -> new NoLineFoundException());
    }

    @Transactional
    public void updateLine(final Long id, final LineRequest lineRequest) {
        Line line = lineRepository.findById(id).orElseThrow(NoLineFoundException::new);
        line.update(lineRequest.toLine());
    }

    @Transactional
    public void deleteLine(final Long id) {
        lineRepository.deleteById(id);
    }

    @Transactional
    public void saveSection(final Long lineId, final SectionRequest sectionRequest) {

        Station upStation = stationRepository.findById(sectionRequest.getUpStationId()).orElseThrow(NoStationFoundException::new);
        Station downStation = stationRepository.findById(sectionRequest.getDownStationId()).orElseThrow(NoStationFoundException::new);
        Line line = lineRepository.findById(lineId).orElseThrow(NoLineFoundException::new);

        if(!line.isValidUpStation(upStation)) {
            throw new InvalidUpStationException();
        }

        if(!line.isValidDownStation(downStation)) {
            throw new InvalidDownStationException();
        }

        line.addSection(upStation, downStation, sectionRequest.getDistance());
    }

    @Transactional
    public void deleteSection(final Long lineId, final Long stationId) {
        Line line = lineRepository.findById(lineId).orElseThrow(NoLineFoundException::new);

        if(line.hasOnlyOneSection()) {
            throw new OnlyOneSectionRemainingException();
        }

        if(!line.isProperStationToDelete(stationId)) {
            throw new InvalidDownStationException();
        }

        line.deleteSection(stationId);
    }
}
