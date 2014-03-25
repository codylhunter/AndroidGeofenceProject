from django.db import models

class Activity (models.Model):
    location = models.CharField(max_length=200)
    timeStamp = models.TimeField(auto_now_add=True)
    startTime = models.TimeField()
    stopTime = models.TimeField()
    date = models.DateField()
    workTime = models.BooleanField()

    def __str__(self):
        return self.location + ": " + str(self.date) + " " + str(self.startTime) + " - " + str(self.stopTime)
        
