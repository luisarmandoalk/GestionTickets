package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.LoginRequest;
import com.campuslands.tickets.dto.TokenResponse;

public interface AutenticacionService {

    TokenResponse iniciarSesion(LoginRequest solicitud);
}
