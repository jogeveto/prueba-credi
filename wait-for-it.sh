#!/bin/bash

# Función para esperar por un servicio
wait_for_service() {
    local host=$1
    local port=$2
    local service=$3
    
    echo "Esperando por $service..."
    while ! nc -z $host $port; do
        echo "$service no está disponible - esperando..."
        sleep 5
    done
    echo "$service está disponible"
}

# Esperar por Oracle
wait_for_service oracle 1521 "Oracle"

# Esperar 30 segundos adicionales para asegurar que Oracle esté completamente iniciado
echo "Esperando 30 segundos adicionales para asegurar que Oracle esté listo..."
sleep 30


echo "Todos los servicios están disponibles. Iniciando la aplicación..."
exec java -Dspring.profiles.active=docker -jar app.jar
