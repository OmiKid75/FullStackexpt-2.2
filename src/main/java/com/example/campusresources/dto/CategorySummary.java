package com.example.campusresources.dto;

import java.io.Serializable;

/** Small category view embedded inside a resource response. */
public record CategorySummary(Long id, String name) implements Serializable {
}
