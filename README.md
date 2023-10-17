# Aplicativo de referência para Mapa (Google Maps) ![Linguagem](https://img.shields.io/badge/Linguagem-Kotlin-blue) ![Versão Mínima do Android](https://img.shields.io/badge/Stack-Android-brightgreen)


Este é um aplicativo de exemplo que demonstra como usar o Google Maps e a busca próxima a locais. Ele permite aos usuários buscar lugares próximos usando a API do Google Maps.

## Recursos

- Mostra o mapa do Google com a capacidade de busca.
- Exibe marcadores para os lugares encontrados na busca.
- Requer permissões de localização e acesso à rede para funcionar corretamente.
- Lida com casos em que a localização do dispositivo e a rede não estão ativadas.
- Usa Retrofit para fazer chamadas à API do Google Places.

## Pré-requisitos

Antes de executar o aplicativo, certifique-se de:

- Ter uma chave de API válida do Google Maps. Substitua a chave em `local.properties` no atributo `MAPS_API_KEY`.
- Configurar as dependências do Retrofit em seu projeto.

## Como Usar

1. Clone este repositório em seu ambiente de desenvolvimento.
2. Certifique-se de que sua chave de API do Google Maps esteja configurada corretamente.
3. Construa e execute o aplicativo em seu dispositivo ou emulador.
4. Autorize as permissões de localização quando solicitado.
5. Use a barra de pesquisa para procurar lugares próximos.

