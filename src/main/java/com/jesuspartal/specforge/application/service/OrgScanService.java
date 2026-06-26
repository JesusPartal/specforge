package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.api.dto.OrgScanResponse;
import com.jesuspartal.specforge.api.dto.OrgScanResponse.RepoWithSpec;
import com.jesuspartal.specforge.api.dto.OrgScanResponse.RepoWithoutSpec;
import com.jesuspartal.specforge.infrastructure.github.GitHubClient;
import com.jesuspartal.specforge.infrastructure.github.GitHubRepoResponse;
import com.jesuspartal.specforge.infrastructure.github.SpecFileFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrgScanService {

    private final GitHubClient gitHubClient;
    private final SpecFileFinder specFileFinder;

    @Cacheable(value = "org-scan", key = "#org")
    public OrgScanResponse scan(String org) {
        List<GitHubRepoResponse> repos = gitHubClient.listOrgRepos(org);
        List<RepoWithSpec> withSpec = new ArrayList<>();
        List<RepoWithoutSpec> withoutSpec = new ArrayList<>();

        for (var repo : repos) {
            String[] parts = repo.fullName().split("/", 2);
            var found = specFileFinder.findSpec(parts[0], parts[1]);
            if (found.isPresent()) {
                withSpec.add(new RepoWithSpec(repo.name(), found.get().path(), repo.htmlUrl()));
            } else {
                withoutSpec.add(new RepoWithoutSpec(repo.name(), repo.htmlUrl()));
            }
        }

        return new OrgScanResponse(org, repos.size(), withSpec, withoutSpec);
    }
}