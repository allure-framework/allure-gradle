const { ALLURE_SERVICE_TOKEN } = process.env;

const allureService =
    ALLURE_SERVICE_TOKEN
        ? {
            accessToken: ALLURE_SERVICE_TOKEN,
        }
        : undefined;

export default {
    name: "Allure Gradle",
    output: "./build/allure-report",
    ...(allureService ? { allureService } : {}),
};
